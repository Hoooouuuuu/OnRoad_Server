// ✅ 전역 상태
let directionPolyline = null;
let directionInfoWindow = null;
let myLocationMarker = null;
let routeClickMarker = null;
let routeClickInfoWindow = null;
let routeActive = false;
let searchTimeout = null;
let startMarker = null;
let goalMarker = null;
let mapClickListener = null;
let popupLocked = false;

// ✅ 출발지/도착지
window.routeStart = { lat: null, lng: null, label: "내 위치" };
window.routeGoal = { lat: null, lng: null, label: "" };

// ✅ 내 위치 출발지로 설정
window.setStartToCurrentLocation = function () {
  if (!navigator.geolocation) return;

  navigator.geolocation.getCurrentPosition(pos => {
    const { latitude: lat, longitude: lng } = pos.coords;
    routeStart = { lat, lng, label: "내 위치" };
    document.getElementById('startPointLabel').value = "내 위치";

    const posLatLng = new naver.maps.LatLng(lat, lng);
    myLocationMarker?.setMap(null);

    myLocationMarker = new naver.maps.Marker({
      position: posLatLng,
      map,
      icon: {
        url: '/image/my-marker.png',
        size: new naver.maps.Size(44, 66),
        anchor: new naver.maps.Point(22, 22)
      },
      zIndex: 999
    });

    map.panTo(posLatLng);
  });
};

// ✅ 지도 클릭 이벤트 등록
window.initRouteEvents = function () {
  if (mapClickListener) return;
  mapClickListener = naver.maps.Event.addListener(map, 'click', e => {
    if (!popupLocked) showRouteChoice(e.coord.lat(), e.coord.lng(), "선택한 위치");
  });
};

// ✅ 지도 클릭 이벤트 해제
window.removeRouteEvents = function () {
  if (mapClickListener) {
    naver.maps.Event.removeListener(mapClickListener);
    mapClickListener = null;
  }
};

// ✅ 출/도 설정 팝업
window.showRouteChoice = function (lat, lng, label) {
  routeClickInfoWindow?.setMap(null);
  routeClickMarker?.setMap(null);

  const position = new naver.maps.LatLng(lat, lng);
  routeClickMarker = new naver.maps.Marker({ position, map });

  const content = document.createElement('div');
  content.className = 'clean-popup';
  content.innerHTML = `
    <div class="popup-title">${label}</div>
    <div class="popup-btn" onclick="setAsStart(${lat}, ${lng}, '${label}')">🚩 출발지로 설정</div>
    <div class="popup-btn" onclick="setAsGoal(${lat}, ${lng}, '${label}')">🎯 도착지로 설정</div>
  `;

  const overlay = new naver.maps.OverlayView();
  overlay.onAdd = function () {
    this.getPanes().overlayLayer.appendChild(content);
  };
  overlay.draw = function () {
    const proj = this.getProjection();
    const point = proj.fromCoordToOffset(position);
    content.style.left = `${point.x - content.offsetWidth / 2}px`;
    content.style.top = `${point.y - content.offsetHeight - 20}px`;
  };
  overlay.onRemove = function () {
    content.remove();
  };
  overlay.setMap(map);
  routeClickInfoWindow = overlay;
};

// ✅ 출발지 설정
window.setAsStart = function (lat, lng, label) {
  if (routeStart.lat === lat && routeStart.lng === lng) {
    routeClickInfoWindow?.setMap(null);
    return;
  }

  popupLocked = true;
  startMarker?.setMap(null);
  routeClickMarker?.setMap(null);

  routeStart = { lat, lng, label };
  document.getElementById('startPointLabel').value = label;

  startMarker = new naver.maps.Marker({
    position: new naver.maps.LatLng(lat, lng),
    map,
    icon: {
      content: `<div style="font-size: 32px;">🚩</div>`,
      anchor: new naver.maps.Point(16, 32)
    }
  });

  routeClickInfoWindow?.setMap(null);
  tryFindRoute();
  setTimeout(() => popupLocked = false, 300);
};

// ✅ 도착지 설정
window.setAsGoal = function (lat, lng, label) {
  if (routeGoal.lat === lat && routeGoal.lng === lng) {
    routeClickInfoWindow?.setMap(null);
    return;
  }

  popupLocked = true;
  goalMarker?.setMap(null);

  routeGoal = { lat, lng, label };
  document.getElementById('goalPointLabel').value = label;

  goalMarker = new naver.maps.Marker({
    position: new naver.maps.LatLng(lat, lng),
    map,
    icon: {
      content: `<div style="font-size: 32px;">🎯</div>`,
      anchor: new naver.maps.Point(16, 32)
    }
  });

  routeClickInfoWindow?.setMap(null);

  if (!routeStart.lat) window.setStartToCurrentLocation();
  tryFindRoute();
  setTimeout(() => popupLocked = false, 300);
};

// ✅ 경로 탐색 시도
function tryFindRoute() {
  if (routeStart.lat && routeGoal.lat) {
    findDirection(routeStart.lat, routeStart.lng, routeGoal.lat, routeGoal.lng);
    routeActive = true;
  }
}

// ✅ 경로 요청
window.findDirection = function (startLat, startLng, goalLat, goalLng) {
  fetch(`/api/proxy/naver-direction?startLat=${startLat}&startLng=${startLng}&goalLat=${goalLat}&goalLng=${goalLng}`)
    .then(res => res.json())
    .then(data => {
      const route = data?.route?.trafast?.[0];
      if (!route?.path) return alert("경로를 찾을 수 없습니다.");

      const path = route.path.map(([lng, lat]) => new naver.maps.LatLng(lat, lng));
      directionPolyline?.setMap(null);
      directionInfoWindow?.close();

      directionPolyline = new naver.maps.Polyline({
        path, map,
        strokeColor: '#0d6efd',
        strokeWeight: 6,
        strokeOpacity: 0.9
      });

      const mid = path[Math.floor(path.length / 2)];
      const durationMin = Math.round(route.summary.duration / 60000);
      directionInfoWindow = new naver.maps.InfoWindow({
        content: `<div style="padding:6px 12px;">🕒 예상 소요: <strong>${durationMin}분</strong></div>`,
        position: mid
      });

      directionInfoWindow.open(map);
      map.panTo(mid);
    })
    .catch(err => {
      console.error("❌ 경로 API 실패:", err);
      alert("경로를 가져올 수 없습니다.");
    });
};

// ✅ 초기화 함수
window.clearRoute = function () {
  directionPolyline?.setMap(null);
  directionInfoWindow?.close();
  directionPolyline = directionInfoWindow = null;
  routeGoal = { lat: null, lng: null, label: "" };
  routeActive = false;
};

window.clearRouteMarkers = function () {
  startMarker?.setMap(null); startMarker = null;
  goalMarker?.setMap(null); goalMarker = null;
  routeClickMarker?.setMap(null); routeClickMarker = null;
  routeClickInfoWindow?.setMap(null); routeClickInfoWindow = null;
};

window.resetRoutePanel = function () {
  document.querySelectorAll('.clean-popup')?.forEach(el => el.remove());
  clearRouteMarkers();
  clearRoute();

  window.routeStart = { lat: null, lng: null, label: "내 위치" };
  window.routeGoal = { lat: null, lng: null, label: "" };

  mapClickListener && naver.maps.Event.removeListener(mapClickListener);
  mapClickListener = null;

  document.getElementById('startPointLabel').value = '';
  document.getElementById('goalPointLabel').value = '';
  document.getElementById('startResultList').innerHTML = '';
  document.getElementById('goalResultList').innerHTML = '';
  document.getElementById('startResultList').style.display = 'none';
  document.getElementById('goalResultList').style.display = 'none';

  setStartToCurrentLocation();
  initRouteEvents();
};

// ✅ 자동완성 기능
function setupAutoComplete(inputId, resultListId, isStart) {
  const input = document.getElementById(inputId);
  const resultList = document.getElementById(resultListId);

  input.addEventListener('input', function () {
    const keyword = this.value.trim();
    clearTimeout(searchTimeout);
    if (!keyword) return (resultList.style.display = 'none', resultList.innerHTML = '');

    searchTimeout = setTimeout(() => {
      fetch(`/api/proxy/kakao-place?query=${encodeURIComponent(keyword)}`)
        .then(res => res.json())
        .then(data => {
          resultList.innerHTML = '';
          if (!data.documents?.length) return (resultList.style.display = 'none');

          data.documents.forEach(place => {
            const li = document.createElement('li');
            li.className = 'list-group-item list-group-item-action';
            li.textContent = place.place_name;
            li.addEventListener('click', () => {
              const lat = parseFloat(place.y);
              const lng = parseFloat(place.x);
              const label = place.place_name;

              input.value = label;
              resultList.innerHTML = '';
              resultList.style.display = 'none';
              map.panTo(new naver.maps.LatLng(lat, lng));
              isStart ? setAsStart(lat, lng, label) : setAsGoal(lat, lng, label);
            });
            resultList.appendChild(li);
          });

          resultList.style.display = 'block';
        });
    }, 300);
  });
}

document.addEventListener("DOMContentLoaded", () => {
  setupAutoComplete('startPointLabel', 'startResultList', true);
  setupAutoComplete('goalPointLabel', 'goalResultList', false);
});
