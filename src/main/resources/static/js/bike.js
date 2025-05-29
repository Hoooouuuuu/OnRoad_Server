// 🚲 전역 상태
let bikeMarkers = [];
let allBikeStations = [];
let bikeRoutePolyline = null;
let bikeRouteLabel = null;
let isBikeRouting = false;

window.userPositionMarker = null;
window.recommendedStation = null;
window.activeInfoWindow = null;
window.userLat = null;
window.userLng = null;
window.skipBikeRecommendation = false;

// ✅ 거리 계산 (Haversine)
function getDistance(lat1, lon1, lat2, lon2) {
  const R = 6371;
  const toRad = deg => deg * Math.PI / 180;
  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
    Math.sin(dLon / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)) * 1000;
}

// ✅ 위치 이동
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
        anchor: new naver.maps.Point(22, 22)
      },
      title: '내 위치',
      zIndex: 999
    });

    map.panTo(userPos);
    window.skipBikeRecommendation = skipRecommendation;
  }, () => alert("위치 정보를 가져올 수 없습니다."));
};

// ✅ 마커 클리어
window.clearBikeStations = function () {
  bikeMarkers.forEach(b => b.marker.setMap(null));
  bikeMarkers = [];
  window.activeInfoWindow?.close();
  window.activeInfoWindow = null;
  bikeRoutePolyline?.setMap(null);
  bikeRouteLabel?.close();
  bikeRoutePolyline = null;
  bikeRouteLabel = null;
};

// ✅ 추천 대여소
window.recommendNearestStation = function () {
  if (!window.userLat || !window.userLng) return;

  const nearby = bikeMarkers
    .map(m => ({
      ...m,
      distance: getDistance(window.userLat, window.userLng, m.position.lat(), m.position.lng())
    }))
    .filter(m => m.distance <= 500)
    .sort((a, b) => a.distance - b.distance);

  if (!nearby.length) {
    alert('500m 이내에 추천 가능한 대여소가 없습니다.');
    return;
  }

  const best = nearby[0];

  bikeMarkers.forEach(b => {
    const count = parseInt(b.station.parkingBikeTotCnt);
    b.marker.setIcon({
      url: getBikeMarkerUrl(count),
      size: new naver.maps.Size(44, 60),
      anchor: new naver.maps.Point(22, 60)
    });
  });

  const count = parseInt(best.station.parkingBikeTotCnt);
  best.marker.setIcon({
    url: getBikeMarkerUrl(count),
    size: new naver.maps.Size(60, 70),
    anchor: new naver.maps.Point(30, 70)
  });

  map.setZoom(17);
  map.panTo(best.position);

  window.recommendedStation = {
    stationLatitude: best.position.lat(),
    stationLongitude: best.position.lng(),
    stationName: best.name,
    rackTotCnt: best.station.rackTotCnt,
    parkingBikeTotCnt: best.station.parkingBikeTotCnt,
    shared: best.station.shared
  };

  window.activeInfoWindow?.close();
  window.activeInfoWindow = null;

  showStationDetailPanel(
    `🚲 ${best.name}`,
    `잔여 자전거: ${best.station.parkingBikeTotCnt}대 / 거치대: ${best.station.rackTotCnt}대`,
    best.distance
  );
};

function getBikeMarkerUrl(count) {
  if (count === 0) return '/image/bike-marker-red.png';
  if (count <= 5) return '/image/bike-marker-yellow.png';
  return '/image/bike-marker-green.png';
}

window.goToNaverRoute = function () {
  if (!window.recommendedStation || !window.userLat || !window.userLng) {
    alert('위치 정보 또는 추천 대여소 정보가 없습니다.');
    return;
  }

  const startLat = window.userLat;
  const startLng = window.userLng;
  const goalLat = window.recommendedStation.stationLatitude;
  const goalLng = window.recommendedStation.stationLongitude;

  fetch(`/api/proxy/naver-direction?startLat=${startLat}&startLng=${startLng}&goalLat=${goalLat}&goalLng=${goalLng}`)
    .then(res => res.json())
    .then(data => {
      const route = data.route?.trafast?.[0];
      if (!route?.path || !route?.summary) {
        alert("경로 정보를 가져올 수 없습니다.");
        return;
      }

      const latlngs = route.path.map(([lng, lat]) => new naver.maps.LatLng(lat, lng));

      // ✅ 경로 그리기
      if (bikeRoutePolyline) bikeRoutePolyline.setMap(null);
      bikeRoutePolyline = new naver.maps.Polyline({
        map: map,
        path: latlngs,
        strokeColor: '#007AFF',
        strokeOpacity: 0.8,
        strokeWeight: 6
      });

      // ✅ 소요 시간 계산
      const durationMs = route.summary.duration; // 밀리초
      const minutes = Math.round(durationMs / 60000);

      // ✅ 도착지에 말풍선 표시
      const end = latlngs[latlngs.length - 1];
      if (bikeRouteLabel) bikeRouteLabel.close();
      bikeRouteLabel = new naver.maps.InfoWindow({
        content: `<div style="font-size:13px;">⏱ 소요시간: 약 ${minutes}분</div>`,
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
      alert("경로를 불러오는 중 오류가 발생했습니다.");
    });
};

// ✅ 경로 취소
window.cancelBikeRoute = function () {
  isBikeRouting = false;
  bikeRoutePolyline?.setMap(null);
  bikeRouteLabel?.close();
  bikeRoutePolyline = null;
  bikeRouteLabel = null;
  window.activeInfoWindow?.close();
  window.activeInfoWindow = null;
  window.recommendedStation = null;
  window.clearBikeStations();
  window.moveToMyLocation();
};

// ✅ 따릉이 API 호출
window.loadBikeStations = function () {
  if (isBikeRouting) return;
  const urls = [
    '/api/proxy/bike-list' // 백엔드 프록시 활용
  ];

  Promise.all(urls.map(url => fetch(url).then(res => res.json())))
    .then(results => {
      allBikeStations = results.flatMap(r => r?.rentBikeStatus?.row || []);
      window.renderVisibleBikeMarkers();
    })
    .catch(err => {
      console.error("❌ 따릉이 API 오류", err);
      alert("따릉이 데이터를 불러오지 못했습니다.");
    });
};

// ✅ 마커 렌더링
window.renderVisibleBikeMarkers = function () {
  const bounds = map.getBounds();
  window.clearBikeStations();

  allBikeStations.forEach(station => {
    const lat = parseFloat(station.stationLatitude);
    const lng = parseFloat(station.stationLongitude);
    if (isNaN(lat) || isNaN(lng)) return;

    const position = new naver.maps.LatLng(lat, lng);
    if (!bounds.hasLatLng(position)) return;

    const name = station.stationName.replace(/^\d+\.\s*/, '');
    const count = parseInt(station.parkingBikeTotCnt);
    const defaultImage = getBikeMarkerUrl(count);
    const hoverImage = `/image/bike-hover/bike-hover-${count > 9 ? '9plus' : count}.png`;

    const imageSize = new naver.maps.Size(44, 60);
    const imageAnchor = new naver.maps.Point(22, 60);

    const marker = new naver.maps.Marker({
      position,
      map,
      icon: { url: defaultImage, size: imageSize, anchor: imageAnchor },
      title: name
    });

    const infoWindow = new naver.maps.InfoWindow({
      content: `<div style="padding:5px; font-size:13px;">${name}</div>`,
      backgroundColor: "#fff",
      borderColor: "#999",
      borderWidth: 1,
      disableAnchor: true
    });

    naver.maps.Event.addListener(marker, 'mouseover', () => {
      infoWindow.open(map, marker);
      marker.setIcon({ url: hoverImage, size: imageSize, anchor: imageAnchor });
    });

    naver.maps.Event.addListener(marker, 'mouseout', () => {
      infoWindow.close();
      marker.setIcon({ url: defaultImage, size: imageSize, anchor: imageAnchor });
    });

    naver.maps.Event.addListener(marker, 'click', () => {
      window.recommendedStation = {
        stationLatitude: lat,
        stationLongitude: lng,
        stationName: name,
        rackTotCnt: station.rackTotCnt,
        parkingBikeTotCnt: station.parkingBikeTotCnt,
        shared: station.shared
      };

      showStationDetailPanel(
        `🚲 ${name}`,
        `잔여 자전거: ${station.parkingBikeTotCnt}대 / 거치대: ${station.rackTotCnt}대`
      );

      window.activeInfoWindow?.close();
      window.activeInfoWindow = null;
    });

    bikeMarkers.push({ marker, name, position, bikeCount: count, station });
  });
};

// ✅ UI 이벤트 연결
(() => {
  document.getElementById("moveToMyLocation")?.addEventListener("click", window.moveToMyLocation);
  document.getElementById("recommendBtn")?.addEventListener("click", () => {
    if (!window.userLat || !window.userLng) {
      alert("먼저 위치를 불러와 주세요.");
      return;
    }
    window.recommendNearestStation();
  });

  document.getElementById("closeDetailPanel")?.addEventListener("click", () => {
    document.getElementById("stationDetailPanel").style.display = "none";
  });

  document.querySelectorAll(".sidebar button").forEach(btn => {
    btn.addEventListener("click", () => {
      const panel = document.getElementById("stationDetailPanel");
      if (panel) panel.style.display = "none";
    });
  });
})();

// ✅ 상세 패널 UI 표시
function showStationDetailPanel(name, info, distance = null) {
  const panel = document.getElementById("stationDetailPanel");
  if (!panel) return;
  document.getElementById("detailStationName").textContent = name;
  document.getElementById("detailStationInfo").textContent = info;
  document.getElementById("detailStationDistance").textContent =
    distance !== null ? `거리: ${Math.round(distance)}m` : "";
  panel.style.display = "block";
}
