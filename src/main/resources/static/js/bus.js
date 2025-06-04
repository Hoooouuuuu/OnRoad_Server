let busMarkers = [];
let busTimer = null;
let stopMarkers = [];
let allStops = [];
let clusterer;
let routeLine = null;
let routeMarkers = [];
window.routeMarkers = routeMarkers;
let arrivalTimers = {};
let visibleStops = [];     // 현재 지도 내 표시되는 정류소
let routeStops = [];       // 검색한 노선의 정류소
let currentRouteId = null; // 현재 활성화된 노선 ID
let nearbyStopMarkers = []; // ✅ 주변 정류소 마커 저장용
window.nearbyStopMarkers = nearbyStopMarkers;
let arrivalAutoRefreshTimer = null;

const typeColorMap = {
  "간선": "bg-primary",
  "지선": "bg-success",
  "광역": "bg-danger",
  "마을": "bg-warning",
  "순환": "bg-info",
  "공항": "bg-dark",
  "경기": "bg-secondary",
  "인천": "bg-secondary",
  "기타": "bg-light text-dark"
};

// 🔹 기존 마커 제거
function clearBusMarkers() {
  busMarkers.forEach(marker => marker.setMap(null));
  busMarkers = [];
}

function getBusIconByTurnaround(bus, stationList) {
  if (!bus.lastStnId || !bus.trnstnid || !Array.isArray(stationList) || stationList.length === 0) {
    // console.warn("❌ 방향 판단 실패: 필수 데이터 없음");
    return defaultIcon("R");
  }

  const getStop = (id) => stationList.find(
    s => s.node_id == id || s.stopId == id || s.station_id == id
  );

  const startStop = stationList.find(s => s.stationOrder == 1);
  const turnStop = getStop(bus.trnstnid);
  const lastStop = getStop(bus.lastStnId);

  if (!startStop || !turnStop || !lastStop) {
    // console.warn("❌ 정류소 매칭 실패:", { startStop, turnStop, lastStop });
    return defaultIcon("R");
  }

  const sx = parseFloat(startStop.lng), sy = parseFloat(startStop.lat);
  const tx = parseFloat(turnStop.lng), ty = parseFloat(turnStop.lat);
  const lx = parseFloat(lastStop.lng), ly = parseFloat(lastStop.lat);

  if ([sx, sy, tx, ty, lx, ly].some(v => isNaN(v))) {
    // console.warn("❌ 좌표 파싱 실패");
    return defaultIcon("R");
  }

  // ✅ 중간선 기준: 출발지가 오른쪽에 있으면 기본은 ←, 왼쪽에 있으면 기본은 →
  const midX = (sx + tx) / 2;
  const defaultDirection = sx > midX ? "L" : "R";

  // ✅ 회차지 통과 여부: stationOrder 기준
  const getOrder = (id) => getStop(id)?.stationOrder ?? null;
  const lastSeq = getOrder(bus.lastStnId);
  const turnSeq = getOrder(bus.trnstnid);
  const passedTurnaround = lastSeq != null && turnSeq != null && lastSeq >= turnSeq;

  const direction = passedTurnaround
    ? (defaultDirection === "L" ? "R" : "L")
    : defaultDirection;

  return {
    url: `/image/bus/icon-bus-${direction}.png`,
    size: new naver.maps.Size(24, 24),
    anchor: new naver.maps.Point(8, 24)
  };
}

function defaultIcon(direction = "R") {
  return {
    url: `/image/bus/icon-bus-${direction}.png`,
    size: new naver.maps.Size(24, 24),
    anchor: new naver.maps.Point(8, 24)
  };
}

async function showBusPositions({ routeId, routeNumber }) {
  let url = '';
  if (routeId) {
    url = `/api/proxy/busPos?routeId=${encodeURIComponent(routeId)}`;
  } else if (routeNumber) {
    url = `/api/proxy/busPosByNumber?routeNumber=${encodeURIComponent(routeNumber)}`;
  } else {
    alert("버스 노선 정보가 부족합니다 (routeId 또는 routeNumber)");
    return;
  }

  try {
    const response = await fetch(url, {
      method: 'GET',
      headers: { 'Accept': 'application/json' }
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`API 호출 실패: ${response.status} ${response.statusText}`);
    }

    const data = await response.json();

    if (!data.msgHeader || data.msgHeader.headerCd !== '0') {
      clearBusMarkers();
      alert('버스 위치 데이터를 가져오지 못했습니다: ' + (data.msgHeader?.headerMsg || '서버 오류'));
      return;
    }

    const itemList = data?.msgBody?.itemList;
    const buses = Array.isArray(itemList) ? itemList : (itemList ? [itemList] : []);

    if (buses.length === 0) {
      clearBusMarkers();
      alert('현재 운행 중인 버스가 없습니다.');
      return;
    }

    clearBusMarkers();

    buses.forEach(bus => {
      const lat = parseFloat(bus.gpsY);
      const lng = parseFloat(bus.gpsX);
      const carNo = bus.vehId;

      if (!isNaN(lat) && !isNaN(lng)) {
        const icon = getBusIconByTurnaround(bus, routeStops);

        const marker = new naver.maps.Marker({
          position: new naver.maps.LatLng(lat, lng),
          map: map,
          title: `버스 번호: ${carNo}`,
          icon
        });

        naver.maps.Event.addListener(marker, 'click', () => {
          info.open(map, marker);
        });

        busMarkers.push(marker);
      }
    });
  } catch (err) {
    clearBusMarkers();
    alert('버스 위치를 불러오는 중 오류가 발생했습니다: ' + err.message);
  }
}

function startBusTracking({ routeId, routeNumber }) {
  if (busTimer) {
    clearInterval(busTimer);
  }

  showBusPositions({ routeId, routeNumber });

  busTimer = setInterval(() => {
    showBusPositions({ routeId, routeNumber });
  }, 10000);
}

function stopBusTracking() {
  if (busTimer) {
    clearInterval(busTimer);
    busTimer = null;
    clearBusMarkers();
  }
}

document.addEventListener("DOMContentLoaded", async () => {
  const btn = document.getElementById('sidebarBusBtn');
  const resetBtn = document.getElementById("resetMapBtn");
  const selector = document.getElementById("regionSelector");

  // ▶ 사이드바 버튼 초기화
  btn?.addEventListener('click', () => {
    stopBusTracking();
    clearStopMarkers();
    clearRouteDisplay();
    currentRouteId = null;
    routeStops = [];

    // ✅ 선택 전에는 allStops 초기화 (정류소 미표시)
    allStops = [];
  });

  // ▶ 초기화 버튼
  resetBtn?.addEventListener("click", () => {
    stopBusTracking();         // 실시간 추적 중지
    clearStopMarkers();        // 정류소 마커 제거
    clearRouteDisplay();       // 노선 경로 및 마커 제거
    clearBusMarkers();         // 버스 마커 제거

    currentRouteId = null;
    routeStops = [];
    allStops = [];

    // 1. 시/도 선택 초기화
    const selector = document.getElementById("regionSelector");
    if (selector) {
      selector.value = "";
    }

    // 2. 버스 번호 입력 초기화
    const input = document.getElementById("routeInput");
    if (input) {
      input.value = "";
    }

    // 3. 도착 정보 패널 초기화
    const arrivalPanel = document.getElementById("arrivalPanelBody");
    if (arrivalPanel) {
      arrivalPanel.innerHTML = `
      <div class="text-muted small py-3 px-2 text-center">
        ※ 시/도를 선택하거나 버스 번호로 검색하세요.
      </div>
    `;
    }

    // 4. 상세정보 팝업 닫기
    const popup = document.getElementById("routeDetailPopup");
    if (popup) {
      popup.classList.add("d-none");
    }

    // 5. 모달 닫기 (노선 목록 모달)
    const routeModal = bootstrap.Modal.getInstance(document.getElementById('routeListModal'));
    if (routeModal) {
      routeModal.hide();
    }

    // 6. 지도 중심을 기본 위치로 이동 (서울 기준)
    const center = cityCenters["서울특별시"];
    if (center && map) {
      map.setCenter(new naver.maps.LatLng(center[0], center[1]));
      map.setZoom(13); // 기본 줌 레벨
    }
  });

  // ▶ 시/도 선택 박스 로딩 및 이벤트
  try {
    const res = await fetch("/api/proxy/bus/regions");
    const cities = await res.json();

    cities.forEach(city => {
      const opt = document.createElement("option");
      opt.value = city;
      opt.textContent = city;
      selector?.appendChild(opt);
    });

    selector?.addEventListener("change", async e => {
      const region = e.target.value;

      stopBusTracking();
      clearStopMarkers();
      clearRouteDisplay();
      currentRouteId = null;
      routeStops = [];

      // ✅ 정류소 표시 전 전체 제거
      allStops = [];

      if (!region) return;

      if (region === '서울특별시') {
        await loadBusStopsByRegion(region); // 정류소 로딩 + 마커 표시 포함
      } else {
        alert(`[${region}] 지역의 정류소 정보는 준비 중입니다.`);
      }
    });

  } catch (e) {
    // console.error("도시 목록 로딩 실패", e);
  }

  // ▶ 지도 이동 시 바운드 내 정류소 마커 갱신
  let idleTimer = null;
  naver.maps.Event.addListener(map, 'idle', () => {
    clearTimeout(idleTimer);
    idleTimer = setTimeout(() => {
      if (map.getZoom() < 12) {
        clearStopMarkers();
        return;
      }

      // ✅ 경로 탐색 중이 아닐 때, 시/도 선택 후 정류소만 표시
      if (!currentRouteId && allStops.length > 0) {
        filterStopsInView();
      }
    }, 300);
  });
});

const cityCenters = {
  '서울특별시': [37.5665, 126.9780],
  '부산광역시': [35.1796, 129.0756],
  '대구광역시': [35.8714, 128.6014],
  '인천광역시': [37.4563, 126.7052],
  '광주광역시': [35.1595, 126.8526],
  '대전광역시': [36.3504, 127.3845],
  '울산광역시': [35.5384, 129.3114],
  '세종특별자치시': [36.4800, 127.2891],
  '경기도': [37.4138, 127.5183],
  '강원특별자치도': [37.8228, 128.1555],
  '충청북도': [36.6357, 127.4917],
  '충청남도': [36.5184, 126.8000],
  '전라북도': [35.7167, 127.1444],
  '전라남도': [34.8161, 126.4630],
  '경상북도': [36.4919, 128.8889],
  '경상남도': [35.4606, 128.2132],
  '제주특별자치도': [33.4996, 126.5312]
};

function clearStopMarkers() {
  stopMarkers.forEach(m => m.setMap(null));
  stopMarkers = [];

  // 🔧 주변 정류소 마커도 함께 제거
  nearbyStopMarkers.forEach(m => m.setMap(null));
  nearbyStopMarkers = [];
}

const normalIcon = {
  url: "/image/bus/bus-stop.png",
  size: new naver.maps.Size(16, 16),
  anchor: new naver.maps.Point(8, 16)
};

const selectedIcon = {
  url: "/image/bus/bus-stop-click.png",
  size: new naver.maps.Size(32, 32),
  anchor: new naver.maps.Point(16, 32)
};

let lastSelectedStopMarker = null;

function drawStopMarkers(stops, isRouteMarkers = false, isNearby = false) {
  // 일반/주변 정류소만 초기화 (노선 마커는 유지)
  if (!isRouteMarkers && !isNearby) clearStopMarkers();

  let index = 0;
  const batchSize = 200;
  const delay = 50;

  function drawBatch() {
    const nextBatch = stops.slice(index, index + batchSize);

    nextBatch.forEach(stop => {
      const lat = parseFloat(stop.lat || stop.latitude);
      const lng = parseFloat(stop.lng || stop.longitude);
      const name = stop.name || stop.stationName;
      const stopId = stop.stopId || stop.nodeId || stop.id;
      const arsId = stop.arsId || "01";

      if (!stopId) {
        // console.warn("❗ 정류소 ID 누락됨", stop);
        return;
      }

      const icon = isRouteMarkers
        ? {
          url: "/image/bus/bus-stop-route.png",
          size: new naver.maps.Size(16, 16),
          anchor: new naver.maps.Point(8, 16)
        }
        : normalIcon;

      const marker = new naver.maps.Marker({
        position: new naver.maps.LatLng(lat, lng),
        map: map,
        title: name,
        icon: icon
      });

      // 클릭 이벤트 등록
      naver.maps.Event.addListener(marker, 'click', () => {
        // console.log("🧭 정류소 클릭:", stopId, arsId);

        if (lastSelectedStopMarker && !isRouteMarkers) {
          lastSelectedStopMarker.setIcon(normalIcon);
        }

        if (!isRouteMarkers) {
          marker.setIcon(selectedIcon);
          lastSelectedStopMarker = marker;
        }

        onBusStopClick(stopId, arsId, name);
      });

      // 마커 저장
      if (isRouteMarkers) {
        window.routeMarkers.push(marker);
      } else if (isNearby) {
        nearbyStopMarkers.push(marker);
      } else {
        stopMarkers.push(marker);
      }
    });

    index += batchSize;
    if (index < stops.length) {
      setTimeout(drawBatch, delay);
    }
  }

  drawBatch();
}

let lastBounds = null;
const MAX_MARKERS = 500;

async function filterStopsInView() {
  if (!map) return;

  const bounds = map.getBounds();
  const sw = bounds.getSW();
  const ne = bounds.getNE();

  try {
    const res = await fetch(`/api/proxy/bus/stops/in-bounds?minLat=${sw.lat()}&maxLat=${ne.lat()}&minLng=${sw.lng()}&maxLng=${ne.lng()}`, {
      method: 'GET',
      headers: {
        'Accept': 'application/json'
      },
      redirect: 'follow' // 또는 'manual'로 리디렉션 방지
    });

    // 🚨 로그인 페이지로 리다이렉션 되었다면, fetch는 응답 본문이 HTML임
    const contentType = res.headers.get("content-type");
    if (contentType && contentType.includes("text/html")) {
      alert("세션이 만료되었거나 로그인이 필요합니다. 다시 로그인해주세요.");
      location.href = "/login";
      return;
    }

    if (!res.ok) {
      throw new Error("서버 응답 오류: " + res.status);
    }

    const stops = await res.json();
    allStops = stops;
    drawStopMarkers(stops.slice(0, 1000));
  } catch (e) {
    // console.error("정류소 로딩 실패", e);
    alert("정류소 정보를 불러오는 데 실패했습니다.");
  }
}

async function loadBusStopsByRegion(region) {
  if (!region) return;

  // ✅ 버스 패널이 꺼져있으면 실행 안 함
  if (!panelStates.bus) {
    // console.warn("버스 패널이 비활성화 상태입니다. 정류소 로딩 중단.");
    return;
  }

  // ✅ 지도 중심 이동
  if (cityCenters[region]) {
    const [lat, lng] = cityCenters[region];
    map.setCenter(new naver.maps.LatLng(lat, lng));
    map.setZoom(17);
  }

  try {
    const res = await fetch(`/api/proxy/bus/stops?region=${encodeURIComponent(region)}`);

    if (!res.ok) {
      throw new Error(`정류소 불러오기 실패: ${res.status}`);
    }

    allStops = await res.json();

    // ✅ 버스 경로가 없는 상태에서만 마커 표시
    if (!currentRouteId && allStops.length > 0 && panelStates.bus) {
      drawStopMarkers(allStops.slice(0, MAX_MARKERS)); // 예: 1000
    }

  } catch (err) {
    // console.error("정류소 불러오기 실패", err);
    alert("정류소 정보를 불러오는 중 문제가 발생했습니다.");
  }
}

function onBusStopClick(stopId, arsId = "01", stopName = "정류소") {
  // 전역 변수에 저장 (자동 새로고침을 위한 정보)
  window.lastStopId = stopId;
  window.lastArsId = arsId;
  window.lastStopName = stopName;

  // 도착 정보 불러오기
  fetch(`/api/proxy/bus/arrivals?stopId=${stopId}&arsId=${arsId}`)
    .then(res => res.json())
    .then(arrivals => {
      showArrivalModal(arrivals, stopName); // ✅ 정류소명 전달
      startArrivalAutoRefresh();            // ✅ 30초 주기 자동 새로고침 시작
    });

  // 정류소를 지나는 노선 목록도 함께 표시
  fetch(`/api/proxy/bus/routes?stopId=${stopId}`)
    .then(res => res.json())
    .then(routes => showRouteListModal(routes));
}

function showArrivalModal(arrivals, stopName = "정류소") {
  const container = document.getElementById("arrivalPanelBody");
  if (!container) return;

  Object.values(arrivalTimers).forEach(clearInterval);
  arrivalTimers = {};

  const grouped = {};
  arrivals.forEach((item, idx) => {
    const routeNumber = item.routeNumber;
    if (!grouped[routeNumber]) grouped[routeNumber] = [];
    grouped[routeNumber].push({ ...item, idx });
  });

  const sortedKeys = Object.keys(grouped).sort((a, b) =>
    a.localeCompare(b, 'ko', { numeric: true })
  );

  const groups = {
    soon: [],
    running: [],
    waiting: [],
    ended: [],
    unknown: []
  };

  container.innerHTML = `<h5 class="mb-3"><i class="bi bi-bus-front-fill me-1"></i>${stopName}</h5>`;

  sortedKeys.forEach(routeNumber => {
    const list = grouped[routeNumber];
    const first = list[0];
    const second = list[1];

    const typeClass = typeColorMap[first.routeType || "기타"] || "bg-light text-dark";
    const sec1 = parseArrivalSeconds(first.arrivalTime);
    const sec2 = second ? parseArrivalSeconds(second.arrivalTime) : null;

    const groupKey = (() => {
      if (first.arrivalTime === "운행 종료") return "ended";
      if (first.arrivalTime === "곧 도착") return "soon";
      if (first.arrivalTime?.includes("대기") || first.arrivalTime?.includes("없음")) return "waiting";
      if (sec1 != null && sec1 <= 60) return "soon";
      if (sec1 != null) return "running";
      return "unknown";
    })();

    const formatTime = sec => {
      if (sec == null) return "-";
      if (sec <= 60) return "곧 도착";
      return `⏱ ${formatArrivalSec(sec)}`;
    };

    const html = `
      <div class="arrival-card border-bottom py-2 arrival-item" data-route="${routeNumber}" style="cursor: pointer;">
        <div class="d-flex align-items-center justify-content-between">
          <div class="bus-number-box ${typeClass} text-white fw-bold text-center me-3"
              style="min-width: 50px; height: 32px; line-height: 32px; border-radius: 4px;">
            ${routeNumber}
          </div>
          <div class="flex-grow-1 d-flex justify-content-between align-items-center small w-100">
            <div style="min-width: 80px;">
              <span id="arrivalTime${first.idx}">${formatTime(sec1)}</span>
              ${first.congestion ? `<span class="badge ${getCongestionBadgeClass(first.congestion)} ms-1">${first.congestion}</span>` : ""}
            </div>
            <div style="min-width: 80px;">
              <span id="arrivalTime${second?.idx ?? 'second'}">${formatTime(sec2)}</span>
              ${second?.congestion ? `<span class="badge ${getCongestionBadgeClass(second.congestion)} ms-1">${second.congestion}</span>` : ""}
            </div>
          </div>
        </div>
      </div>
    `;

    groups[groupKey].push({
      html,
      idx: first.idx,
      sec1,
      sec2,
      routeNumber,
      secondIdx: second?.idx
    });
  });

  const renderGroup = (title, className, list) => {
    if (list.length === 0) return;
    const wrapper = document.createElement("div");
    wrapper.innerHTML = `<div class="${className} fw-bold mb-2 mt-3">${title}</div>` +
      list.map(e => e.html).join('');
    container.appendChild(wrapper);
  };

  renderGroup("🚨 곧 도착", "text-danger", groups.soon);
  renderGroup("🟢 운행 중", "text-success", groups.running);
  renderGroup("⏳ 운행 대기", "text-warning", groups.waiting);
  renderGroup("⛔ 운행 종료", "text-muted", groups.ended);

  let activeTimers = 0;
  let finishedTimers = 0;

  const timingList = [...groups.soon, ...groups.running];
  if (timingList.length === 0) return;

  timingList.forEach(({ idx, sec1, sec2, routeNumber, secondIdx }) => {
    // 첫 번째 도착시간
    if (sec1 != null) {
      activeTimers++;
      let currentSec1 = sec1;
      const timeEl1 = document.getElementById(`arrivalTime${idx}`);

      const intervalId1 = setInterval(() => {
        if (!timeEl1) {
          clearInterval(intervalId1);
          return;
        }

        currentSec1--;

        if (currentSec1 <= 60) {
          timeEl1.textContent = "곧 도착";  // ✅ 항상 곧 도착으로
        } else {
          timeEl1.textContent = `⏱ ${formatArrivalSec(currentSec1)}`;
        }

        if (currentSec1 <= 0) {
          clearInterval(intervalId1);
          finishedTimers++;
          if (finishedTimers >= activeTimers) {
            setTimeout(() => reloadArrivals({ delay: true }), 5000);
          }
        }
      }, 1000);
      arrivalTimers[idx] = intervalId1;
    }

    // 두 번째 도착시간
    if (secondIdx && sec2 != null) {
      let currentSec2 = sec2;
      const timeEl2 = document.getElementById(`arrivalTime${secondIdx}`);

      const intervalId2 = setInterval(() => {
        if (!timeEl2) {
          clearInterval(intervalId2);
          return;
        }

        currentSec2--;

        if (currentSec2 <= 60) {
          timeEl2.textContent = "곧 도착"; // ✅ 무조건 곧 도착
        } else {
          timeEl2.textContent = `⏱ ${formatArrivalSec(currentSec2)}`;
        }

        if (currentSec2 <= 0) {
          clearInterval(intervalId2); // ✅ 도착 표시 없음
        }
      }, 1000);
      arrivalTimers[secondIdx] = intervalId2;
    }
  });
}

function startArrivalAutoRefresh() {
  if (arrivalAutoRefreshTimer) clearInterval(arrivalAutoRefreshTimer);

  arrivalAutoRefreshTimer = setInterval(() => {
    if (window.lastStopId && window.lastArsId) {
      // console.log("🔁 자동 도착 정보 새로고침 실행");
      fetch(`/api/proxy/bus/arrivals?stopId=${window.lastStopId}&arsId=${window.lastArsId}`)
        .then(res => res.json())
        .then(arrivals => {
          showArrivalModal(arrivals, window.lastStopName || "정류소");
        });
    }
  }, 10000); // ⏱ 10초 간격
}

function reloadArrivals({ delay = true } = {}) {
  const stopId = window.lastStopId;
  const arsId = window.lastArsId;
  const stopName = window.lastStopName || "정류소";

  if (!stopId || !arsId) return;

  // 도착 정보 먼저 갱신
  fetch(`/api/proxy/bus/arrivals?stopId=${stopId}&arsId=${arsId}`)
    .then(res => res.json())
    .then(arrivals => showArrivalModal(arrivals, stopName));

  // 정류소 경유 노선도 갱신
  const refreshRoutes = () => {
    fetch(`/api/proxy/bus/routes?stopId=${stopId}`)
      .then(res => res.json())
      .then(routes => showRouteListModal(routes));
  };

  if (delay) {
    setTimeout(refreshRoutes, 5000); // 5초 지연 후
  } else {
    refreshRoutes(); // 즉시
  }
}

function formatArrivalSec(sec) {
  if (sec < 60) return `${sec}초`;
  const min = Math.floor(sec / 60);
  const remain = sec % 60;
  return `${min}분 ${remain}초`;
}

function makeHtml(idx, routeNumber, typeClass, statusText, congestionClass, item) {
  return `
    <div class="arrival-card border-bottom py-2 arrival-item" data-route="${routeNumber}">
      <div class="d-flex justify-content-between align-items-center">
        <div class="d-flex align-items-center flex-grow-1">
          <div class="bus-number-box ${typeClass} text-white fw-bold text-center me-2"
              style="min-width: 50px; height: 32px; line-height: 32px; border-radius: 4px;">
            ${routeNumber}
          </div>
          <div class="flex-grow-1">
            <div class="d-flex justify-content-between small">
              <div id="arrivalTime${idx}" class="${congestionClass}">
                ${statusText}
              </div>
              <div class="${congestionClass}" style="min-width: 50px; text-align: right;">
                ${item.congestion || '정보 없음'}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `;
}

function getCongestionBadgeClass(text) {
  if (text === "여유") return "bg-success text-white";
  if (text === "보통") return "bg-warning text-dark";
  if (text === "혼잡") return "bg-danger text-white";
  return "bg-secondary text-white";
}

// 🔧 이벤트 위임으로 상세 표시
document.body.addEventListener('click', e => {
  const target = e.target.closest('.arrival-item');
  if (target && target.dataset.route) {
    const route = target.dataset.route;
    loadRouteDetail(route, target); // 💡 상세정보 패널 띄우기
  }
});

function showRouteListModal(routes) {
  const container = document.getElementById("routeListModalBody");
  if (!container) return;

  if (!Array.isArray(routes) || routes.length === 0) {
    container.innerHTML = "<p>이 정류장을 경유하는 버스가 없습니다.</p>";
  } else {
    container.innerHTML = routes.map(route => `
      <div class="d-flex justify-content-between align-items-center border-bottom py-2">
        <div>
          <strong>${route.routeNumber}</strong>
          <span class="text-muted">(${route.routeType})</span>
        </div>
        <button class="btn btn-sm btn-primary" onclick="onRouteSelected('${route.routeId}')">실시간 위치</button>
      </div>
    `).join('');
  }

  const modal = new bootstrap.Modal(document.getElementById('routeListModal'));
  modal.show();
}

function onRouteSelected(routeId) {
  stopBusTracking();
  startBusTracking({ routeId });
  loadRouteDetail(null, routeId);  // ✅ routeId 직접 전달
}

function clearRouteDisplay() {
  if (window.routeLine) {
    window.routeLine.setMap(null);
    window.routeLine = null;
  }

  if (Array.isArray(window.routeMarkers)) {
    window.routeMarkers.forEach(marker => marker.setMap(null));
    window.routeMarkers = [];
  }

  // 🆕 내부 상태 초기화
  window.currentRouteId = null;
  window.routeStops = [];

  const arrivalPanel = document.getElementById("arrivalPanelBody");
  if (arrivalPanel) {
    arrivalPanel.innerHTML = `<div class="text-muted small py-3 px-2 text-center">
      ※ 시/도를 선택하거나 버스 번호로 검색하세요.
    </div>`;
  }
}

window.searchBusRoute = async function () {
  const input = document.getElementById("routeInput");
  const routeNumber = input?.value?.trim();

  if (!routeNumber) {
    alert("버스 번호를 입력해주세요.");
    return;
  }

  stopBusTracking();      // 실시간 추적 중지
  clearStopMarkers();     // 정류소 마커 제거
  clearRouteDisplay();    // 이전 경로 제거
  currentRouteId = null;

  try {
    const res = await fetch(`/api/proxy/bus/routes?routeNumber=${encodeURIComponent(routeNumber)}`);
    const stops = await res.json();

    if (stops.length === 0) {
      alert("해당 버스 노선 정보를 찾을 수 없습니다.");
      return;
    }

    // ✅ 1. 지도에 표시
    const path = stops.map(stop =>
      new naver.maps.LatLng(parseFloat(stop.lat), parseFloat(stop.lng))
    );

    // --- 아래 코드 삽입 시작 (도로 중심선 기반 경로로 대체하는 경우) ---
    // ⚠️ 시연용 도로 중심선 경로 Polyline 시각화 (네이버 API 이용, 사용 시 주석 해제)
    /* 
    try {
      const start = stops[0];
      const end = stops[stops.length - 1];
      const drivingRes = await fetch(`/api/proxy/naver-driving-path?startLat=${start.lat}&startLng=${start.lng}&goalLat=${end.lat}&goalLng=${end.lng}`);
      const drivingPath = await drivingRes.json();
    
      if (window.routeLine) window.routeLine.setMap(null);
    
      window.routeLine = new naver.maps.Polyline({
        path: drivingPath.map(p => new naver.maps.LatLng(p.lat, p.lng)),
        strokeColor: '#ff0000',
        strokeWeight: 5,
        map: map
      });
    } catch (e) {
      // console.error("🛑 도로 중심선 경로 호출 실패", e);
    }
     */
    // --- 코드 삽입 끝 ---

    // 원래 경로 표시

    if (window.routeLine) {
      window.routeLine.setMap(null);
    }
    window.routeLine = new naver.maps.Polyline({
      path: path,
      strokeColor: '#0078ff',
      strokeWeight: 4,
      map: map
    });
    

    // ✅ 2. 지도 위치를 경로 중앙으로 이동
    const bounds = new naver.maps.LatLngBounds();
    path.forEach(p => bounds.extend(p));
    map.fitBounds(bounds); // 👈 경로가 전부 보이도록 줌 조정

    // ✅ 3. 정류소 마커 표시 (노선용, 지도용)
    drawStopMarkers(stops, true);       // 노선 마커
    // drawStopMarkers(visibleStops);   // ❌ 이거 호출하면 방금 표시한 마커 덮임!!

    // ✅ 4. 실시간 버스 위치 추적
    startBusTracking({ routeNumber });

    currentRouteId = routeNumber;
    routeStops = stops;

  } catch (err) {
    // console.error("버스 경로 조회 실패", err);
    alert("버스 노선 정보를 불러오는 데 실패했습니다.");
  }
};

async function loadRouteDetail(routeNumber, triggerEl) {
  try {
    const res = await fetch(`/api/proxy/bus/detail?routeNumber=${routeNumber}`);

    // ✅ 404 등 비정상 응답 처리
    if (!res.ok) {
      const error = await res.json();
      alert(`버스 상세 정보 요청 실패: ${error?.error || res.statusText}`);
      return;
    }

    const data = await res.json();
    // console.log("📦 상세정보 응답:", data);

    const html = `
      <div class="fw-bold mb-1">${data?.routeNumber || '알 수 없음'}번 버스</div>
      <div>🕒 배차: ${data?.interval || '정보 없음'}</div>
      <div>🚏 첫차: ${data?.firstTime || '정보 없음'}</div>
      <div>🌙 막차: ${data?.lastTime || '정보 없음'}</div>
      <div class="mt-2 text-end">
        <button class="btn btn-sm btn-outline-primary" onclick="openBusRoutePanel('${data?.routeNumber || ''}')">
          노선 보기
        </button>
      </div>
    `;

    const popup = document.getElementById('routeDetailPopup');
    const content = document.getElementById('routeDetailPopupContent');
    content.innerHTML = html;

    // 위치 고정
    const rect = triggerEl.getBoundingClientRect();
    popup.style.top = `${window.scrollY + 60}px`;
    popup.style.right = `20px`;

    popup.classList.remove('d-none');
  } catch (err) {
    // console.error("상세 정보 불러오기 실패", err);
    alert("버스 상세 정보를 불러오는 중 오류 발생: " + err.message);
  }
}

async function loadArrivalAtStop(stopId, arsId) {
  try {
    const res = await fetch(`/api/proxy/bus/arrivals?stopId=${stopId}&arsId=${arsId}`);
    const arrivals = await res.json();

    const stopElem = [...document.querySelectorAll(`#busStopListContainer .border-bottom`)]
      .find(div => div.innerHTML.includes(stopId));
    if (!stopElem) return;

    const arrivalHtml = arrivals.map(arrival => `
      <div class="small text-primary mt-1">
        🚌 ${arrival.routeNumber} → ${arrival.arrivalTime} (${arrival.congestion})
      </div>
    `).join("");

    stopElem.insertAdjacentHTML('beforeend', arrivalHtml);
  } catch (e) {
    // console.error("도착 정보 불러오기 오류", e);
    alert("도착 정보를 불러오는 중 오류 발생");
  }
}

document.body.addEventListener('click', async e => {
  const target = e.target.closest('.arrival-item');
  if (!target || !target.dataset.route) return;

  const routeNumber = target.dataset.route;
  // console.log("🚌 도착 리스트에서 선택한 노선:", routeNumber);

  try {
    // ❗ 절대로 지우지 마세요: 정류소 마커, 리스트, 팝업
    stopBusTracking();     // 기존 추적 종료
    clearBusMarkers();     // 기존 버스 마커만 제거 (정류소 마커는 그대로)

    // 👉 방향 판단을 위한 정류소 목록만 갱신
    const res = await fetch(`/api/proxy/bus/routes?routeNumber=${encodeURIComponent(routeNumber)}`);
    const stops = await res.json();

    if (!Array.isArray(stops) || stops.length === 0) {
      alert("정류소 정보를 불러올 수 없습니다.");
      return;
    }

    routeStops = stops;
    currentRouteId = routeNumber;

    // 👉 방향 포함된 실시간 마커 표시
    startBusTracking({ routeNumber });

  } catch (err) {
    // console.error("❌ 도착 리스트 클릭 처리 오류:", err);
    alert("버스 위치를 표시할 수 없습니다.");
  }
});


document.addEventListener("click", function (e) {
  const popup = document.getElementById("routeDetailPopup");
  if (!popup.contains(e.target) && !e.target.classList.contains("route-detail-btn")) {
    popup.classList.add("d-none");
  }
});

function parseArrivalSeconds(arrivalText) {
  if (!arrivalText) return null;
  if (arrivalText.includes("도착")) return 0;
  if (arrivalText.includes("곧 도착")) return 30;

  const full = arrivalText.match(/^(\d+)\s*분\s*(\d+)?\s*초?/);
  if (full) {
    const min = parseInt(full[1], 10);
    const sec = full[2] ? parseInt(full[2], 10) : 0;
    return min * 60 + sec;
  }

  const secOnly = arrivalText.match(/^(\d+)\s*초$/);
  if (secOnly) return parseInt(secOnly[1], 10);

  const minOnly = arrivalText.match(/^(\d+)\s*분$/);
  if (minOnly) return parseInt(minOnly[1], 10) * 60;

  return null;
}

async function openBusRoutePanel(routeNumber) {
  if (!routeNumber) return;

  stopBusTracking();
  clearStopMarkers();
  clearRouteDisplay();
  currentRouteId = null;

  try {
    const res = await fetch(`/api/proxy/bus/routes?routeNumber=${encodeURIComponent(routeNumber)}`);
    const stops = await res.json();

    if (!Array.isArray(stops) || stops.length === 0) {
      alert("해당 노선의 정류소 정보를 찾을 수 없습니다.");
      return;
    }

    const path = stops.map(stop =>
      new naver.maps.LatLng(parseFloat(stop.lat), parseFloat(stop.lng))
    );
    
    // 1️⃣ 노선 경로 폴리라인
/* 
    // --- 아래 코드 삽입 시작 (도로 중심선 기반 경로로 대체하는 경우) ---
    // ⚠️ 시연용 도로 중심선 경로 Polyline 시각화 (네이버 API 이용, 사용 시 주석 해제)
    try {
      const start = stops[0];
      const end = stops[stops.length - 1];
      const drivingRes = await fetch(`/api/proxy/naver-driving-path?startLat=${start.lat}&startLng=${start.lng}&goalLat=${end.lat}&goalLng=${end.lng}`);
      const drivingPath = await drivingRes.json();
    
      if (window.routeLine) window.routeLine.setMap(null);
    
      window.routeLine = new naver.maps.Polyline({
        path: drivingPath.map(p => new naver.maps.LatLng(p.lat, p.lng)),
        strokeColor: '#ff0000',
        strokeWeight: 5,
        map: map
      });
    } catch (e) {
      // console.error("🛑 도로 중심선 경로 호출 실패", e);
    }
     */
    // --- 코드 삽입 끝 ---

    // 원래 경로 표시

    if (window.routeLine) {
      window.routeLine.setMap(null);
    }
    window.routeLine = new naver.maps.Polyline({
      path: path,
      strokeColor: '#0078ff',
      strokeWeight: 4,
      map: map
    });
    

    // 2️⃣ 경로 기준 지도 확대
    const bounds = new naver.maps.LatLngBounds();
    path.forEach(p => bounds.extend(p));
    map.fitBounds(bounds);

    // 3️⃣ 노선 정류소 마커
    drawStopMarkers(stops, true);

    // 4️⃣ 실시간 버스 위치 추적 시작
    startBusTracking({ routeNumber });

    currentRouteId = routeNumber;
    routeStops = stops;

    // 5️⃣ 상세 정보 패널 닫기 (선택사항)
    document.getElementById("routeDetailPopup")?.classList.add("d-none");

  } catch (err) {
    // console.error("노선 보기 실패", err);
    alert("노선 정보를 불러오는 중 오류가 발생했습니다.");
  }
}

window.findNearbyStops = async function () {
  if (!navigator.geolocation) {
    alert("이 브라우저에서는 위치 정보가 지원되지 않습니다.");
    return;
  }

  navigator.geolocation.getCurrentPosition(async position => {
    const lat = position.coords.latitude;
    const lng = position.coords.longitude;

    // 지도 이동
    map.setCenter(new naver.maps.LatLng(lat, lng));
    map.setZoom(18);

    // 사용자 위치 마커
    if (window.userPositionMarker) {
      window.userPositionMarker.setMap(null);
    }
    window.userPositionMarker = new naver.maps.Marker({
      position: new naver.maps.LatLng(lat, lng),
      map: map,
      icon: {
        url: '/image/my-marker.png',
        size: new naver.maps.Size(44, 66),
        anchor: new naver.maps.Point(22, 22)
      },
      title: '내 위치'
    });

    // 주변 정류소 호출
    const res = await fetch(`/api/proxy/bus/stops/nearby?lat=${lat}&lng=${lng}&radius=500`);

    const contentType = res.headers.get("content-type");
    if (contentType && contentType.includes("text/html")) {
      alert("세션이 만료되었거나 로그인이 필요합니다. 다시 로그인해주세요.");
      location.href = "/login";
      return;
    }

    if (!res.ok) {
      alert("정류소를 불러오는 데 실패했습니다.");
      return;
    }

    const stops = await res.json();
    if (!Array.isArray(stops) || stops.length === 0) {
      alert("주변에 정류소가 없습니다.");
      return;
    }

    clearStopMarkers();          // 기존 정류소 마커 제거
    clearRouteDisplay();         // 노선 제거
    clearNearbyStopMarkers();    // 기존 주변 정류소 마커 제거

    // 마커 표시
    drawStopMarkers(stops, false, true); // isNearby = true

    allStops = stops;

  }, error => {
    // console.error("위치 정보 오류:", error);
    alert("위치 정보를 가져오는 데 실패했습니다.");
  });
};

function clearRouteMarkers() {
  if (Array.isArray(window.routeMarkers)) {
    window.routeMarkers.forEach(m => m.setMap(null));
    window.routeMarkers = [];
  }
};

function clearNearbyStopMarkers() {
  if (Array.isArray(window.nearbyStopMarkers)) {
    window.nearbyStopMarkers.forEach(m => m.setMap(null));
    window.nearbyStopMarkers = [];
  }
};

function resetBusStopPanel() {
  // ✅ 1. 자동 새로고침 중단 가장 먼저!
  if (arrivalAutoRefreshTimer) {
    clearInterval(arrivalAutoRefreshTimer);
    arrivalAutoRefreshTimer = null;
  }

  // ✅ 2. 전역 변수 초기화
  window.lastStopId = null;
  window.lastArsId = null;
  window.lastStopName = null;

  // ✅ 3. 패널/마커/노선/입력 초기화
  clearStopMarkers();
  clearRouteDisplay();
  clearNearbyStopMarkers();

  const busInfoList = document.getElementById("busInfoList");
  if (busInfoList) {
    busInfoList.innerHTML = "<p>🚍 도착 정보</p><p class='text-muted'>※ 시/도를 선택하거나 버스 번호로 검색하세요.</p>";
  }

  const arrivalPanel = document.getElementById("arrivalPanelBody");
  if (arrivalPanel) {
    arrivalPanel.innerHTML = `<div class="text-muted small py-3 px-2 text-center">
      ※ 시/도를 선택하거나 버스 번호로 검색하세요.
    </div>`;
  }
}

// 전역 등록
window.clearRouteMarkers = clearRouteMarkers;
window.loadRouteDetail = loadRouteDetail;
window.openBusRoutePanel = openBusRoutePanel;
window.loadArrivalAtStop = loadArrivalAtStop;
window.startBusTracking = startBusTracking;
window.stopBusTracking = stopBusTracking;
window.clearBusMarkers = clearBusMarkers;
window.showBusPositions = showBusPositions;
window.clearRouteDisplay = clearRouteDisplay;
window.resetBusStopPanel = resetBusStopPanel;