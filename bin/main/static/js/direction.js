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

window.userLocation = null;

// ✅ 출발지/도착지
window.routeStart = { lat: null, lng: null, label: "내 위치" };
window.routeGoal = { lat: null, lng: null, label: "" };

// ✅ 내 위치 출발지로 설정
window.setStartToCurrentLocation = function () {
  if (!navigator.geolocation) return;

  navigator.geolocation.getCurrentPosition(pos => {
    const { latitude: lat, longitude: lng } = pos.coords;

    routeStart = { lat, lng, label: "내 위치" };
    window.userLocation = { lat, lng };

    // ✅ 출발 마커 제거 (출발 마커가 있다면)
    startMarker?.setMap(null);
    startMarker = null;

    // ✅ 내 위치 마커 생성
    myLocationMarker?.setMap(null);
    myLocationMarker = new naver.maps.Marker({
      position: new naver.maps.LatLng(lat, lng),
      map,
      icon: {
        url: '/image/my-marker.png',
        size: new naver.maps.Size(44, 66),
        anchor: new naver.maps.Point(22, 66)
      },
      zIndex: 999
    });

    map.panTo(new naver.maps.LatLng(lat, lng));
    searchNearbyPlaces();
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
window.showRouteChoice = function (lat, lng, label, address = '', category = '', phone = '') {
  // 기존 마커 및 팝업 제거
  routeClickInfoWindow?.setMap(null);
  routeClickMarker?.setMap(null);

  const position = new naver.maps.LatLng(lat, lng);

  // 📍 마커 생성
  routeClickMarker = new naver.maps.Marker({
    position,
    map,
    icon: {
      url: '/image/pin_icon.png',               // 경로 주의
      size: new naver.maps.Size(32, 32),        // 실제 이미지 크기
      anchor: new naver.maps.Point(16, 32)      // ⬅️ 하단 중심으로 정렬
    }
  });

  // 📦 팝업 요소 생성
  const content = document.createElement('div');
  content.className = 'clean-popup';
  content.style.position = 'absolute';

  // 📄 HTML 템플릿 구성
  content.innerHTML = `
    <div class="popup-header">
      <div class="popup-title">${label}</div>
      ${category ? `<div class="popup-category">${category}</div>` : ''}
      ${address ? `<div class="popup-address">${address}</div>` : ''}
      ${phone ? `<div class="popup-phone">${phone}</div>` : ''}
    </div>
    <div class="popup-actions">
      <button class="popup-btn" onclick="setAsStart(${lat}, ${lng}, '${label}')">출발</button>
      <button class="popup-btn" onclick="setAsGoal(${lat}, ${lng}, '${label}')">도착</button>
    </div>
  `;

  // 🧭 오버레이 생성
  const overlay = new naver.maps.OverlayView();
  overlay.onAdd = function () {
    this.getPanes().floatPane.appendChild(content);
  };

  overlay.draw = function () {
    const proj = this.getProjection();
    const point = proj.fromCoordToOffset(position);

    // 📌 위치 계산 시점 지연 (DOM 렌더 완료 후)
    setTimeout(() => {
      const verticalOffset = 40;
      content.style.left = `${point.x - content.offsetWidth / 2}px`;
      content.style.top = `${point.y - content.offsetHeight - verticalOffset}px`;
    });
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
    popupLocked = false;
    return;
  }

  popupLocked = true;
  startMarker?.setMap(null);
  routeClickMarker?.setMap(null);

  routeStart = { lat, lng, label };

  startMarker = new naver.maps.Marker({
    position: new naver.maps.LatLng(lat, lng),
    map,
    icon: {
      url: '/image/start_icon.png',
      size: new naver.maps.Size(37, 66),
      anchor: new naver.maps.Point(22, 66)
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
    popupLocked = false;
    return;
  }

  popupLocked = true;
  goalMarker?.setMap(null);
  routeGoal = { lat, lng, label };

  // ✅ 도착지 마커 생성
  goalMarker = new naver.maps.Marker({
    position: new naver.maps.LatLng(lat, lng),
    map,
    icon: {
      url: '/image/finish_icon.png',
      size: new naver.maps.Size(44, 78),
      anchor: new naver.maps.Point(22, 78)
    }
  });

  routeClickInfoWindow?.setMap(null);

  // ✅ 출발지 설정이 없으면 → 내 위치를 출발지로 설정 + 마커 전환
  if (!startMarker) {
    const user = window.userLocation;
    if (user?.lat && user?.lng) {
      routeStart = { lat: user.lat, lng: user.lng, label: "내 위치" };

      // ⛔ 내 위치 마커 제거
      myLocationMarker?.setMap(null);
      myLocationMarker = null;

      // ✅ 출발 마커 추가
      startMarker?.setMap(null);
      startMarker = new naver.maps.Marker({
        position: new naver.maps.LatLng(user.lat, user.lng),
        map,
        icon: {
          url: '/image/start_icon.png',
          size: new naver.maps.Size(37, 66),
          anchor: new naver.maps.Point(22, 66)
        }
      });
    } else {
      alert("현재 위치 정보를 먼저 받아야 출발지를 설정할 수 있습니다.");
      popupLocked = false;
      return;
    }
  }

  tryFindRoute();
  setTimeout(() => popupLocked = false, 300);
};

function tryFindRoute() {
  if (routeStart.lat && routeGoal.lat) {
    findDirection(routeStart.lat, routeStart.lng, routeGoal.lat, routeGoal.lng);
    routeActive = true;
  }
}

window.findDirection = function (startLat, startLng, goalLat, goalLng) {
  fetch(`/api/proxy/naver-direction?startLat=${startLat}&startLng=${startLng}&goalLat=${goalLat}&goalLng=${goalLng}`)
    .then(res => res.json())
    .then(data => {
      const route = data?.route?.trafast?.[0];
      if (!route?.path) return alert("경로를 찾을 수 없습니다.");

      const path = route.path.map(([lng, lat]) => new naver.maps.LatLng(lat, lng));
      const mid = path[Math.floor(path.length / 2)];
      const durationMin = Math.round(route.summary.duration / 60000);
      const durationText = formatDuration(durationMin);

      directionPolyline?.setMap(null);
      directionInfoWindow?.setMap(null);

      directionPolyline = new naver.maps.Polyline({
        path,
        map,
        strokeColor: '#0d6efd',
        strokeWeight: 6,
        strokeOpacity: 0.9
      });

      // ✅ 예상 소요 시간 박스
      const box = document.createElement('div');
      box.className = 'clean-popup direction-duration-popup';
      box.style.position = 'absolute';
      box.innerHTML = `
        <div class="popup-header text-center">
          <div class="popup-title">🕒 예상 소요</div>
          <div class="popup-duration">${durationText}</div>
        </div>
      `;

      const overlay = new naver.maps.OverlayView();
      overlay.onAdd = function () {
        this.getPanes().floatPane.appendChild(box);
      };
      overlay.draw = function () {
        const proj = this.getProjection();
        const point = proj.fromCoordToOffset(mid);
        box.style.left = `${point.x - box.offsetWidth / 2}px`;
        box.style.top = `${point.y - box.offsetHeight - 40}px`;
      };
      overlay.onRemove = function () {
        box.remove();
      };
      overlay.setMap(map);
      directionInfoWindow = overlay;

      // ✅ 1. 경로 전체 보기 (출발 ~ 도착 포함)
      const bounds = new naver.maps.LatLngBounds();
      path.forEach(coord => bounds.extend(coord));
      map.fitBounds(bounds); // ← 이게 핵심

      // ✅ 2. 중심을 mid로 이동해서 팝업 박스가 중앙에 뜨게 함
      setTimeout(() => map.panTo(mid), 500); // fitBounds 후 약간 딜레이 줘야 안정적

    })
    .catch(err => {
      console.error("❌ 경로 API 실패:", err);
      alert("경로를 가져올 수 없습니다.");
      popupLocked = false;
    });
};

function formatDuration(min) {
  if (min < 60) return `${min}분 소요`;
  const hours = Math.floor(min / 60);
  const mins = min % 60;
  return mins === 0
    ? `${hours}시간 소요`
    : `${hours}시간 ${mins}분 소요`;
}

window.clearRoute = function () {
  directionPolyline?.setMap(null);
  directionInfoWindow?.setMap(null);
  directionPolyline = directionInfoWindow = null;
  routeGoal = { lat: null, lng: null, label: "" };
  routeActive = false;
};

window.clearRouteMarkers = function () {
  startMarker?.setMap(null); startMarker = null;
  goalMarker?.setMap(null); goalMarker = null;
  routeClickMarker?.setMap(null); routeClickMarker = null;
  routeClickInfoWindow?.setMap(null); routeClickInfoWindow = null;

  myLocationMarker?.setMap(null);
  myLocationMarker = null;
};

window.resetRoutePanel = function () {
  clearRouteMarkers();
  clearRoute();
  routeStart = { lat: null, lng: null, label: "내 위치" };
  routeGoal = { lat: null, lng: null, label: "" };
  mapClickListener && naver.maps.Event.removeListener(mapClickListener);
  mapClickListener = null;
  document.getElementById('nearbyPlaceList').innerHTML = '<div class="text-muted">장소를 검색하세요.</div>';
  // window.setStartToCurrentLocation();
};

window.searchNearbyPlaces = function () {
  const category = document.getElementById('categorySelect')?.value || 'FD6';
  const { lat, lng } = window.userLocation || {};
  if (!lat || !lng) return console.warn("❌ 위치 정보 없음");
  fetchNearbyPlaces(lat, lng, category, lat, lng);
};

window.searchFromMap = function () {
  const center = map.getCenter();
  const category = document.getElementById('categorySelect')?.value || 'FD6';
  const user = window.userLocation;

  if (!user) return alert("먼저 내 위치를 받아야 합니다.");

  // ✅ 검색은 지도 중심 기준, 거리 계산은 내 위치 기준
  fetchNearbyPlaces(
    center.lat(),
    center.lng(),    // 검색 기준
    category,
    user.lat,         // ✅ 거리 표기 기준
    user.lng
  );
};

function fetchNearbyPlaces(targetLat, targetLng, category, baseLat, baseLng) {
  fetch(`/api/proxy/kakao-nearby?lat=${targetLat}&lng=${targetLng}&category=${category}&radius=5000`)
    .then(res => res.json())
    .then(data => displayNearbyPlaces(data, baseLat, baseLng))
    .catch(err => {
      console.error("❌ 장소 검색 실패:", err);
      alert("장소 정보를 가져올 수 없습니다.");
    });
}

function displayNearbyPlaces(data, baseLat, baseLng) {
  const list = document.getElementById('nearbyPlaceList');
  list.innerHTML = '';

  const places = data.documents.map(place => {
    const dist = getDistance(baseLat, baseLng, parseFloat(place.y), parseFloat(place.x));
    return { ...place, distance: dist };
  }).sort((a, b) => a.distance - b.distance);

  if (!places.length) {
    list.innerHTML = '<div class="text-muted">주변 장소가 없습니다.</div>';
    return;
  }

  places.forEach(place => {
    const div = document.createElement('div');
    div.className = 'border-bottom py-2';
    const distStr = formatDistance(place.distance);

    div.innerHTML = `
      <strong>${place.place_name}</strong><br/>
      <span class="text-muted">${place.road_address_name || place.address_name}</span><br/>
      <small class="distance-text">${distStr}</small>
    `;

    div.onclick = () => {
      const lat = parseFloat(place.y);
      const lng = parseFloat(place.x);
      map.setZoom(17);
      map.panTo(new naver.maps.LatLng(lat, lng));
      showRouteChoice(
        lat,
        lng,
        place.place_name,
        place.road_address_name || place.address_name || '',
        place.category_name || '',
        place.phone || ''
      );
    };
    list.appendChild(div);
  });
}

function getDistance(lat1, lng1, lat2, lng2) {
  const R = 6371000;
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLng = (lng2 - lng1) * Math.PI / 180;
  const a = Math.sin(dLat / 2) ** 2 +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
    Math.sin(dLng / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function formatDistance(m) {
  return m >= 1000 ? `약 ${(m / 1000).toFixed(1)}km` : `약 ${Math.round(m)}m`;
}

document.getElementById('placeSearchInput').addEventListener('input', function () {
  const keyword = this.value.trim();
  const resultList = document.getElementById('placeSearchResults');
  if (!keyword) return (resultList.innerHTML = '');

  fetch(`/api/proxy/kakao-place?query=${encodeURIComponent(keyword)}`)
    .then(res => res.json())
    .then(data => {
      resultList.innerHTML = '';
      data.documents.forEach(place => {
        const li = document.createElement('li');
        li.className = 'list-group-item list-group-item-action';
        li.textContent = `${place.place_name} (${place.road_address_name || place.address_name})`;
        li.onclick = () => {
          const lat = parseFloat(place.y);
          const lng = parseFloat(place.x);
          const label = place.place_name;
          map.panTo(new naver.maps.LatLng(lat, lng));
          showRouteChoice(lat, lng, label);
          resultList.innerHTML = '';
        };
        resultList.appendChild(li);
      });
    });
});

document.addEventListener('DOMContentLoaded', () => {
  const categorySelect = document.getElementById('categorySelect');

  categorySelect?.addEventListener('change', () => {
    const selectedOption = categorySelect.options[categorySelect.selectedIndex];
    const categoryLabel = selectedOption?.text || '장소';

    // ✅ 결과 제목 변경
    const title = document.getElementById('resultTitle');
    if (title) title.innerText = `📌 주변 ${categoryLabel}`;

    // ✅ 장소 검색 실행
    searchFromMap();
  });
});