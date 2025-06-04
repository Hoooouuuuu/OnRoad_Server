let eventMarkers = [];
let latestEventData = [];
let skipNextIdle = false;
let detailOverlay = null;

// ✅ 마커 전체 제거 + 상세 박스 제거
window.clearEventMarkers = function () {
  eventMarkers.forEach(m => m.setMap(null));
  eventMarkers = [];

  detailOverlay?.setMap(null);
  detailOverlay = null;

  const box = document.getElementById('eventInfoBox');
  if (box) box.style.display = 'none';
};

// ✅ 마커 로딩 및 클릭 이벤트 등록
window.loadEventMarkers = function (eventData) {
  if (!eventData?.body?.items) return;

  window.clearEventMarkers();
  latestEventData = eventData.body.items;

  latestEventData.forEach((event, index) => {
    const lat = parseFloat(event.coordY);
    const lng = parseFloat(event.coordX);
    if (isNaN(lat) || isNaN(lng)) return;

    const marker = new naver.maps.Marker({
      position: new naver.maps.LatLng(lat, lng),
      map,
      icon: {
        url: getEventIcon(event.eventType, event.eventDetailType),
        size: new naver.maps.Size(44, 66),
        anchor: new naver.maps.Point(22, 66)
      }
    });

    naver.maps.Event.addListener(marker, 'click', () => {
      skipNextIdle = true;
      map.panTo(marker.getPosition());

      window.clearEventMarkers();
      marker.setMap(map);
      eventMarkers.push(marker);

      showCustomBox(event, marker.getPosition());
    });

    eventMarkers.push(marker);
  });
};

// ✅ 상세 오버레이 박스 생성
function showCustomBox(event, latLng) {
  detailOverlay?.setMap(null);

  const div = document.createElement('div');
  div.className = 'custom-event-box';
  div.innerHTML = `
    <strong>${event.roadName} (${event.roadNo})</strong><br/>
    🔸 ${event.eventType} ${event.eventDetailType || ''}<br/>
    🕓 ${formatDate(event.startDate)}<br/>
    💬 ${event.message || '정보 없음'}
  `;

  detailOverlay = new naver.maps.OverlayView();
  detailOverlay.onAdd = function () {
    this.getPanes().overlayLayer.appendChild(div);
  };
  detailOverlay.draw = function () {
    const proj = this.getProjection();
    const point = proj.fromCoordToOffset(latLng);
    div.style.left = (point.x - 100) + 'px';
    div.style.top = (point.y + 10) + 'px';
  };
  detailOverlay.onRemove = () => div.remove();
  detailOverlay.setMap(map);
}

// ✅ 아이콘 URL 결정
function getEventIcon(eventType, eventDetailType) {
  const iconMap = {
    detail: {
      '강풍': '/image/event/event-squall.png',
      '차량증가/정체': '/image/event/event-traffic_congestion.png',
      '지정체': '/image/event/event-traffic_congestion.png',
      '시설물보수작업': '/image/event/event-facility_maintenance.png',
      '이벤트/홍보': '/image/event/event-promotion.png',
      '고장': '/image/event/event-vehicle_breakdown.png'
    },
    type: {
      '기상': '/image/event/event-weather.png',
      '재난': '/image/event/event-disaster.png',
      '공사': '/image/event/event-work.png',
      '교통사고': '/image/event/event-accident.png',
      '기타돌발': '/image/event/event-default.png'
    }
  };

  const detail = normalize(eventDetailType);
  const type = normalize(eventType);

  return iconMap.detail[detail] || iconMap.type[type] || '/image/event/event-default.png';
}

function normalize(str) {
  return str?.trim().replace(/["']/g, '');
}

function formatDate(str) {
  return str?.length === 14
    ? `${str.slice(0, 4)}-${str.slice(4, 6)}-${str.slice(6, 8)} ${str.slice(8, 10)}:${str.slice(10, 12)}`
    : '-';
}

// ✅ 오른쪽 패널 구성
window.renderEventListPanel = function (events) {
  const container = document.getElementById('eventListContent');
  if (!container) return;

  latestEventData = events;
  container.innerHTML = events.map((event, i) => `
    <div class="event-card p-2 border bg-white rounded shadow-sm"
        data-index="${i}" style="cursor: pointer;">
      <div class="fw-bold text-primary">${event.roadName} (${event.roadNo})</div>
      <div class="small text-muted">📌 ${event.eventType}${event.eventDetailType ? ` - ${event.eventDetailType}` : ''}</div>
      <div class="small text-muted">🕓 ${formatDate(event.startDate)}</div>
      <div class="small text-truncate mt-1">💬 ${event.message || '정보 없음'}</div>
    </div>
  `).join('');
};

// ✅ Hover 시 마커 1개 표시
document.getElementById('eventListContent')?.addEventListener('mouseover', e => {
  const card = e.target.closest('.event-card');
  if (!card) return;

  const event = latestEventData[parseInt(card.dataset.index)];
  if (!event) return;

  window.clearEventMarkers();

  const marker = new naver.maps.Marker({
    position: new naver.maps.LatLng(parseFloat(event.coordY), parseFloat(event.coordX)),
    map,
    icon: {
      url: getEventIcon(event.eventType, event.eventDetailType),
      size: new naver.maps.Size(44, 66),
      anchor: new naver.maps.Point(22, 66)
    }
  });
  eventMarkers.push(marker);
});

// ✅ 클릭 시 상세 표시
document.getElementById('eventListContent')?.addEventListener('click', e => {
  const card = e.target.closest('.event-card');
  if (!card) return;

  const event = latestEventData[parseInt(card.dataset.index)];
  if (!event) return;

  skipNextIdle = true;

  const latLng = new naver.maps.LatLng(parseFloat(event.coordY), parseFloat(event.coordX));
  map.panTo(latLng);

  window.clearEventMarkers();

  const marker = new naver.maps.Marker({
    position: latLng,
    map,
    icon: {
      url: getEventIcon(event.eventType, event.eventDetailType),
      size: new naver.maps.Size(44, 66),
      anchor: new naver.maps.Point(22, 66)
    }
  });

  eventMarkers.push(marker);
  showCustomBox(event, latLng);
});

// ✅ 지도 화면 내 이벤트 불러오기
window.loadRoadEventsInView = function () {
  if (!panelStates.event) return;

  const bounds = map.getBounds();
  const sw = bounds.getSW();
  const ne = bounds.getNE();

  fetch(`/api/proxy/road-event?minX=${sw.lng()}&minY=${sw.lat()}&maxX=${ne.lng()}&maxY=${ne.lat()}`)
    .then(res => res.json())
    .then(data => {
      window.loadEventMarkers(data);
      window.renderEventListPanel(data.body.items);
    })
    .catch(err => console.error("❌ 도로 이벤트 로딩 실패", err));
};

// ✅ 지도 이동 시 자동 재로딩
document.addEventListener('DOMContentLoaded', () => {
  const waitForMap = setInterval(() => {
    if (window.map) {
      clearInterval(waitForMap);

      // 서울시청 기준 초기 중심 좌표
      const seoulCityHall = new naver.maps.LatLng(37.5665, 126.9780);

      // 맵이 완전히 로드된 후에 줌 및 중심 설정
      naver.maps.Event.once(map, 'tilesloaded', () => {
        map.setCenter(seoulCityHall);
        map.setZoom(11);
      });

      // idle 이벤트 등록
      naver.maps.Event.addListener(map, 'idle', () => {
        if (skipNextIdle) return (skipNextIdle = false);
        if (panelStates.event) {
          window.clearEventMarkers();
          window.loadRoadEventsInView();
        }
      });
    }
    if (panelStates.event) {
      window.clearEventMarkers();
      window.loadRoadEventsInView();
    }
  }, 100);
});