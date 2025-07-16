// 🚲 전역 상태
let bikeMarkers = [];
let allBikeStations = [];
let bikeRoutePolyline = null;
let bikeRouteLabel = null;
let isBikeRouting = false;
let isMapInteracted = false;
let deferredPopupTarget = null; // ⬆️ 클릭된 마커를 기억

window.userPositionMarker = null;
window.recommendedStation = null;
window.activeInfoWindow = null;
window.userLat = null;
window.userLng = null;
window.skipBikeRecommendation = false;

function formatDistance(meter) {
  return meter >= 1000
    ? `${(meter / 1000).toFixed(1)}km`
    : `${meter}m`;
}

function formatWalkingTime(distance) {
  const walkingTimeSec = Math.round(distance / (4000 / 3600));
  const minutes = Math.floor(walkingTimeSec / 60);
  const seconds = walkingTimeSec % 60;
  return minutes > 0
    ? `도보 약 ${minutes}분 ${seconds}초`
    : `도보 약 ${seconds}초`;
}

// ✅ 아이콘 경로 반환
function getBikeMarkerUrl(count) {
  if (count === 0) return '/image/bike-marker-red.png';
  if (count <= 5) return '/image/bike-marker-yellow.png';
  return '/image/bike-marker-green.png';
}

// ✅ 거리 계산
function getDistance(lat1, lon1, lat2, lon2) {
  const R = 6371;
  const toRad = deg => deg * Math.PI / 180;
  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)) * 1000;
}

// ✅ 리스트 갱신 throttle
let updateListTimer = null;
function throttledUpdateNearbyBikeStationList() {
  if (updateListTimer) return;
  updateListTimer = setTimeout(() => {
    updateNearbyBikeStationList();
    updateListTimer = null;
  }, 500);
}

// ✅ 지도 이벤트
naver.maps.Event.addListener(map, 'dragstart', () => { isMapInteracted = true; });
naver.maps.Event.addListener(map, 'zoom_changed', () => { isMapInteracted = true; });
naver.maps.Event.addListener(map, 'idle', () => {
  if (isMapInteracted) {
    renderVisibleBikeMarkers();
    throttledUpdateNearbyBikeStationList();
    isMapInteracted = false;
  }
});

// ✅ 내 위치로 이동
window.moveToMyLocation = function (skipRecommendation = false) {
  if (!navigator.geolocation) return alert("위치 정보를 지원하지 않습니다.");

  navigator.geolocation.getCurrentPosition(pos => {
    const { latitude, longitude } = pos.coords;
    window.userLat = latitude;
    window.userLng = longitude;

    const userPos = new naver.maps.LatLng(latitude, longitude);

    if (window.userPositionMarker) window.userPositionMarker.setMap(null);

    window.userPositionMarker = new naver.maps.Marker({
      position: userPos,
      map,
      icon: {
        url: '/image/my-marker.png',
        size: new naver.maps.Size(44, 66),
        anchor: new naver.maps.Point(22, 66)
      },
      title: '내 위치',
      zIndex: 999
    });

    map.setCenter(userPos);
    map.setZoom(18);
    window.skipBikeRecommendation = skipRecommendation;

    fetch('/api/proxy/bike-list')
      .then(res => res.json())
      .then(data => {
        allBikeStations = data?.rentBikeStatus?.row || [];
        renderVisibleBikeMarkers();
      })
      .catch(err => {
        console.error("❌ 따릉이 API 오류", err);
        alert("따릉이 데이터를 불러오지 못했습니다.");
      });
  }, () => alert("위치 정보를 가져오지 못했습니다."));
};

// ✅ 마커 클리어
window.clearBikeStations = function () {
  const anchoredMarker = window.activeInfoWindow?.getAnchor?.();

  bikeMarkers.forEach(b => {
    if (anchoredMarker === b.marker) return;
    b.marker.setMap(null);
  });

  bikeMarkers = bikeMarkers.filter(b => b.marker === anchoredMarker);

  bikeRoutePolyline?.setMap(null);
  bikeRouteLabel?.close();
  bikeRoutePolyline = null;
  bikeRouteLabel = null;
};

// ✅ 추천 대여소 자동 실행
function autoRecommendNearestStation() {
  if (!window.userLat || !window.userLng) return;

  const nearby = bikeMarkers
    .map(m => ({
      ...m,
      distance: getDistance(window.userLat, window.userLng, m.position.lat(), m.position.lng()),
      count: parseInt(m.station.parkingBikeTotCnt)
    }))
    .filter(m => m.distance <= 500 && m.count > 0)
    .sort((a, b) => a.distance - b.distance);

  if (!nearby.length) return;
  const best = nearby[0];
  setRecommendedStation(best);
}

// ✅ 상태 설정 + 패널 갱신
function setRecommendedStation(m) {
  window.recommendedStation = {
    stationLatitude: m.position.lat(),
    stationLongitude: m.position.lng(),
    stationName: m.name,
    rackTotCnt: m.station.rackTotCnt,
    parkingBikeTotCnt: m.station.parkingBikeTotCnt,
    shared: m.station.shared
  };
  updateBikeStationInfoPanel();
}

function updateBikeStationInfoPanel() {
  const box = document.getElementById("bikeStationInfoBody");
  if (!box) return;

  const s = window.recommendedStation;
  if (!s) {
    box.innerHTML = `<div class="text-muted">추천 대여소 정보가 없습니다.</div>`;
    return;
  }

  const distance = Math.round(getDistance(window.userLat, window.userLng, s.stationLatitude, s.stationLongitude));

  box.innerHTML = `
    <div><strong>${s.stationName}</strong></div>
    <div>📍 거리: ${distance}m</div>
    <div>🚲 자전거: ${s.parkingBikeTotCnt}대</div>
    <div>🅿️ 거치대: ${s.rackTotCnt}대</div>
    <div>📈 가용률: ${s.shared}%</div>
    <div class="mt-2"><button class="btn btn-outline-primary btn-sm" onclick="goToNaverRoute()">길찾기</button></div>
  `;
}

// ✅ 경로 보기
window.goToNaverRoute = function () {
  const s = window.recommendedStation;
  if (!s || !window.userLat || !window.userLng) {
    alert('위치 정보 또는 추천 대여소 정보가 없습니다.');
    return;
  }

  fetch(`/api/proxy/naver-direction?startLat=${window.userLat}&startLng=${window.userLng}&goalLat=${s.stationLatitude}&goalLng=${s.stationLongitude}`)
    .then(res => res.json())
    .then(data => {
      const route = data.route?.trafast?.[0];
      if (!route?.path || !route?.summary) {
        alert("경로 정보를 가져올 수 없습니다.");
        return;
      }

      const latlngs = route.path.map(([lng, lat]) => new naver.maps.LatLng(lat, lng));

      bikeRoutePolyline?.setMap(null);
      bikeRoutePolyline = new naver.maps.Polyline({
        map,
        path: latlngs,
        strokeColor: '#007AFF',
        strokeOpacity: 0.8,
        strokeWeight: 6
      });

      const minutes = Math.round(route.summary.duration / 60000);
      const end = latlngs.at(-1);

      bikeRouteLabel?.close();
      bikeRouteLabel = new naver.maps.InfoWindow({
        content: `<div style="font-size:13px;">⏱ 약 ${minutes}분 소요</div>`,
        backgroundColor: "#fff",
        borderColor: "#007AFF",
        borderWidth: 1,
        disableAnchor: true
      });
      bikeRouteLabel.open(map, end);
      map.panTo(end);
      map.setZoom(15);
    })
    .catch(err => {
      console.error("❌ 경로 API 오류", err);
      alert("경로를 불러오는 중 오류 발생");
    });
};

function cancelBikeRoute() {
  isBikeRouting = false;
  bikeRoutePolyline?.setMap(null);
  bikeRouteLabel?.close();
  bikeRoutePolyline = null;
  bikeRouteLabel = null;
  window.activeInfoWindow?.setMap(null);
  window.activeInfoWindow = null;
  window.recommendedStation = null;
  clearBikeStations();
  moveToMyLocation();
}
window.cancelBikeRoute = cancelBikeRoute;

// ✅ 초기화
function resetBikePanel() {
  cancelBikeRoute();
}
window.resetBikePanel = resetBikePanel;

// ✅ 따릉이 마커 렌더링
window.loadBikeStations = function () {
  if (isBikeRouting) return;

  fetch('/api/proxy/bike-list')
    .then(res => res.json())
    .then(data => {
      allBikeStations = data?.rentBikeStatus?.row || [];
      renderVisibleBikeMarkers();
    })
    .catch(err => {
      console.error("❌ 따릉이 API 오류", err);
      alert("따릉이 데이터를 불러오지 못했습니다.");
    });
};

// ✅ 마커 렌더링 시, 줌이 작을 땐 100개 제한 / 클 땐 30개만 표시
function renderVisibleBikeMarkers() {
  clearBikeStations();
  const zoom = map.getZoom();
  const countLimit = zoom >= 17 ? 30 : 100;

  const bounds = map.getBounds();

  const nearby = allBikeStations
    .map(station => {
      const lat = parseFloat(station.stationLatitude);
      const lng = parseFloat(station.stationLongitude);
      const position = new naver.maps.LatLng(lat, lng);
      const name = station.stationName.replace(/^\d+\.\s*/, '');
      const count = parseInt(station.parkingBikeTotCnt);
      const distance = (window.userLat && window.userLng)
        ? getDistance(window.userLat, window.userLng, lat, lng)
        : 0;

      return { station, lat, lng, position, name, count, distance };
    })
    .filter(m =>
      m.count > 0 &&              // 대여소에 자전거가 있어야 하고
      bounds.hasLatLng(m.position) // 현재 지도 화면 안에 있어야 함 ✅
    )
    .sort((a, b) => a.distance - b.distance)
    .slice(0, countLimit);

  nearby.forEach(m => {
    const marker = new naver.maps.Marker({
      position: m.position,
      map,
      icon: {
        url: getBikeMarkerUrl(m.count),
        size: new naver.maps.Size(44, 60),
        anchor: new naver.maps.Point(22, 60)
      },
      title: m.name
    });

    // 🔁 마커 hover 아이콘
    naver.maps.Event.addListener(marker, 'mouseover', () => {
      marker.setIcon({
        url: getBikeHoverIcon(m.count),
        size: new naver.maps.Size(44, 60),
        anchor: new naver.maps.Point(22, 60)
      });
    });

    naver.maps.Event.addListener(marker, 'mouseout', () => {
      marker.setIcon({
        url: getBikeMarkerUrl(m.count),
        size: new naver.maps.Size(44, 60),
        anchor: new naver.maps.Point(22, 60)
      });
    });

    naver.maps.Event.addListener(marker, 'click', () => {
      window.activeInfoWindow?.setMap(null);

      marker.setIcon({
        url: getBikeHoverIcon(m.count),
        size: new naver.maps.Size(44, 60),
        anchor: new naver.maps.Point(22, 60)
      });

      const distance = Math.round(m.distance);
      const formattedDistance = formatDistance(distance);
      const walkingText = formatWalkingTime(distance);

      const content = document.createElement('div');
      content.className = 'clean-popup';
      content.style.position = 'absolute';

      content.innerHTML = `
        <div class="popup-header">
          <div class="popup-title">${m.name}</div>
          <div class="popup-category" style="font-size:15px; color:#0d6efd;">
            📍 ${formattedDistance} <span style="color:#555; font-size:13px;">(${walkingText})</span>
          </div>
          <div class="popup-address">🚲 ${m.count}대 / 🅿️ ${m.station.rackTotCnt}대</div>
        </div>
        <div class="popup-actions">
          <button class="popup-btn" onclick="goToNaverRoute()">길찾기</button>
        </div>
      `;

      const overlay = new naver.maps.OverlayView();
      overlay.onAdd = function () {
        this.getPanes().floatPane.appendChild(content);
      };

      overlay.draw = function () {
        const proj = this.getProjection();
        const point = proj.fromCoordToOffset(m.position);
        setTimeout(() => {
          const verticalOffset = 60;
          content.style.left = `${point.x - content.offsetWidth / 2}px`;
          content.style.top = `${point.y - content.offsetHeight - verticalOffset}px`;
        });
      };

      overlay.onRemove = function () {
        content.remove();
        marker.setIcon({
          url: getBikeMarkerUrl(m.count),
          size: new naver.maps.Size(44, 60),
          anchor: new naver.maps.Point(22, 60)
        });
      };

      overlay.setMap(map);
      window.activeInfoWindow = overlay;
    });

    bikeMarkers.push({ ...m, marker });
  });

  updateNearbyBikeStationList();
  autoRecommendNearestStation();

  // ✅ 지연된 팝업 처리
  if (deferredPopupTarget) {
    setTimeout(() => {
      const found = bikeMarkers.find(b => b.station.stationId === deferredPopupTarget);
      if (found) {
        const marker = found.marker;
        const distance = Math.round(found.distance);
        const formattedDistance = formatDistance(distance);
        const walkingText = formatWalkingTime(distance);

        const content = document.createElement('div');
        content.className = 'clean-popup';
        content.style.position = 'absolute';

        content.innerHTML = `
          <div class="popup-header">
            <div class="popup-title">${found.name}</div>
            <div class="popup-category" style="font-size:15px; color:#0d6efd;">
              📍 ${formattedDistance} <span style="color:#555; font-size:13px;">(${walkingText})</span>
            </div>
            <div class="popup-address">🚲 ${found.count}대 / 🅿️ ${found.station.rackTotCnt}대</div>
          </div>
          <div class="popup-actions">
            <button class="popup-btn" onclick="goToNaverRoute()">길찾기</button>
          </div>
        `;

        const overlay = new naver.maps.OverlayView();
        overlay.onAdd = function () {
          this.getPanes().floatPane.appendChild(content);
        };
        overlay.draw = function () {
          const proj = this.getProjection();
          const point = proj.fromCoordToOffset(found.position);
          setTimeout(() => {
            const verticalOffset = 60;
            content.style.left = `${point.x - content.offsetWidth / 2}px`;
            content.style.top = `${point.y - content.offsetHeight - verticalOffset}px`;
          });
        };
        overlay.onRemove = function () {
          content.remove();
          marker.setIcon({
            url: getBikeMarkerUrl(found.count),
            size: new naver.maps.Size(44, 60),
            anchor: new naver.maps.Point(22, 60)
          });
        };

        overlay.setMap(map);
        window.activeInfoWindow?.setMap(null);
        window.activeInfoWindow = overlay;
      }

      deferredPopupTarget = null;
    }, 300);
  }
}

function updateNearbyBikeStationList() {
  const listEl = document.getElementById("bikeStationList");
  if (!listEl || !window.userLat || !window.userLng) return;

  const sorted = bikeMarkers
    .map(m => ({
      ...m,
      distance: getDistance(window.userLat, window.userLng, m.position.lat(), m.position.lng()),
      count: parseInt(m.station.parkingBikeTotCnt)
    }))
    .filter(m => m.count > 0)
    .sort((a, b) => a.distance - b.distance)
    .slice(0, 30);

  if (!sorted.length) {
    listEl.innerHTML = '<div class="text-muted">📭 대여 가능한 자전거가 없습니다.</div>';
    return;
  }

  listEl.innerHTML = '';

  sorted.forEach(m => {
    const item = document.createElement('div');
    item.className = 'border-bottom py-2';
    item.style.cursor = 'pointer';
    item.innerHTML = `
      <div class="fw-bold">${m.name}</div>
      <div class="text-muted small">🚲 ${m.count}대 | 📍 ${Math.round(m.distance)}m</div>
    `;

    item.addEventListener('click', () => {
      deferredPopupTarget = m.station.stationId;
      map.panTo(m.position);
      map.setZoom(17);
      renderVisibleBikeMarkers();
    });

    listEl.appendChild(item);
  });
}

window.getBikeHoverIcon = function (count) {
  const clamped = Math.max(0, Math.min(count, 9));
  return `/image/bike-hover/bike-hover-${clamped}${count > 9 ? 'plus' : ''}.png`;
};