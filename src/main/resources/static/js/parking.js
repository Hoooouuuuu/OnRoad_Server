// ✅ 시간 포맷 함수
function formatTime(str) {
  if (!str || str.length < 4) return "-";
  return str.slice(0, 2) + ":" + str.slice(2);
}

// ✅ 상태별 아이콘 결정 함수
function getParkingStatusIcon(p) {
  const capacity = p.capacity || 1; // 나눗셈 방지
  const available = Math.max(p.capacity - p.currentCount, 0);
  const ratio = available / capacity;

  if (available === 0) return '/image/parking/red-icon.png';          // 🔴 만차
  if (ratio < 0.3) return '/image/parking/yellow-icon.png';           // 🟡 혼잡
  return '/image/parking/green-icon.png';                             // 🟢 원활
}

function showParkingLegend() {
  document.getElementById("parkingLegendBox")?.style.setProperty('display', 'block');
}

function hideParkingLegend() {
  document.getElementById("parkingLegendBox")?.style.setProperty('display', 'none');
}

// ✅ 서울시 구별 중심 좌표 정의
const guCenterMap = {
  "강남구": { lat: 37.5172, lng: 127.0473 },
  "강동구": { lat: 37.5301, lng: 127.1238 },
  "강북구": { lat: 37.6396, lng: 127.0256 },
  "강서구": { lat: 37.5509, lng: 126.8495 },
  "관악구": { lat: 37.4784, lng: 126.9516 },
  "광진구": { lat: 37.5384, lng: 127.0823 },
  "구로구": { lat: 37.4954, lng: 126.8874 },
  "금천구": { lat: 37.4603, lng: 126.9009 },
  "노원구": { lat: 37.6542, lng: 127.0568 },
  "도봉구": { lat: 37.6688, lng: 127.0472 },
  "동대문구": { lat: 37.5744, lng: 127.0396 },
  "동작구": { lat: 37.5124, lng: 126.9393 },
  "마포구": { lat: 37.5663, lng: 126.9014 },
  "서대문구": { lat: 37.5791, lng: 126.9368 },
  "서초구": { lat: 37.4836, lng: 127.0326 },
  "성동구": { lat: 37.5633, lng: 127.0360 },
  "성북구": { lat: 37.5894, lng: 127.0167 },
  "송파구": { lat: 37.5145, lng: 127.1056 },
  "양천구": { lat: 37.5170, lng: 126.8666 },
  "영등포구": { lat: 37.5263, lng: 126.8962 },
  "용산구": { lat: 37.5324, lng: 126.9907 },
  "은평구": { lat: 37.6027, lng: 126.9291 },
  "종로구": { lat: 37.5729, lng: 126.9794 },
  "중구": { lat: 37.5636, lng: 126.9976 },
  "중랑구": { lat: 37.6063, lng: 127.0928 }
};

let parkingMarkers = [];

window.clearParkingMarkers = function () {
  parkingMarkers.forEach(m => {
    if (m._infoWindow) m._infoWindow.close();
    m.setMap(null);
  });
  parkingMarkers = [];
};

// ✅ 마커 렌더링 함수
function renderParkingMarkers(filteredList) {
  clearParkingMarkers();
  const allList = window.parkingListData || [];

  allList.forEach(p => {
    const isFilteredIn = filteredList.some(item => item.id === p.id);
    const lat = parseFloat(p.lat);
    const lng = parseFloat(p.lng);
    if (isNaN(lat) || isNaN(lng)) return;

    const iconUrl = isFilteredIn
      ? getParkingStatusIcon(p) // 원활/혼잡/만차
      : '/image/parking/gray-icon.png'; // 필터 제외

    const marker = new naver.maps.Marker({
      map,
      position: new naver.maps.LatLng(lat, lng),
      title: p.name,
      icon: {
        url: iconUrl,
        size: new naver.maps.Size(28, 40),
        anchor: new naver.maps.Point(14, 40)
      }
    });

    if (isFilteredIn) {
      const available = Math.max(p.capacity - p.currentCount, 0);
      const tel = p.tel || "정보 없음";

      const weekdayRaw = formatTime(p.weekdayStart) + " ~ " + formatTime(p.weekdayEnd);
      const holidayRaw = formatTime(p.holidayStart) + " ~ " + formatTime(p.holidayEnd);

      const isWeekdayUnknown = weekdayRaw === "00:00 ~ 00:00";
      const isHolidayUnknown = holidayRaw === "00:00 ~ 00:00";

      const weekdayTime = weekdayRaw === "00:00 ~ 24:00" ? "24시간 운영" : weekdayRaw;
      const holidayTime = holidayRaw === "00:00 ~ 24:00" ? "24시간 운영" : holidayRaw;

      const baseCharge = p.baseCharge || 0;
      const baseMinutes = p.baseMinutes || 0;
      const addCharge = p.addCharge || 0;
      const addMinutes = p.addMinutes || 0;
      const showMaxCharge = p.dayMaxCharge != null && p.dayMaxCharge > 0;
      const maxCharge = showMaxCharge ? `${p.dayMaxCharge.toLocaleString()}원/일 최대` : "";

      const info = new naver.maps.InfoWindow({
        content: `
    <div class="parking-infowindow-container">
      <!-- HEADER: 주차장 이름 -->
      <div class="d-flex justify-content-between align-items-center mb-2">
        <div class="parking-name">
          <i class="bi bi-p-circle-fill me-1"></i>${p.name}
        </div>
      </div>

      <!-- LOCATION: 주소 / 연락처 -->
      <div class="mb-2">
        <div class="text-muted small mb-1">
          <i class="bi bi-geo-alt me-1 text-secondary"></i>${p.address}
        </div>
        <div class="text-muted small">
          <i class="bi bi-telephone me-1 text-secondary"></i>${tel}
        </div>
      </div>

      <hr class="my-2"/>

      <!-- CHARGES: 요금 정보 -->
      <div class="mb-2">
        <div class="section-title"><i class="bi bi-cash-coin me-1"></i>요금</div>
        ${p.payType === "무료" ? `
            <span class="badge bg-success">무료</span><br/>
            <small class="text-muted">* 실제 요금과 다를 수 있습니다.</small>
          ` : `
            <div class="small text-muted">기본요금: <strong>${baseCharge.toLocaleString()}원</strong> / ${baseMinutes}분</div>
            <div class="small text-muted">추가요금: <strong>${addCharge.toLocaleString()}원</strong> / ${addMinutes}분</div>
            ${showMaxCharge ? `<div class="small text-muted">일최대: <strong>${maxCharge}</strong></div>` : ""}
          `
          }
      </div>

      <hr class="my-2"/>

      <!-- HOURS: 운영시간 -->
      <div class="mb-2">
        <div class="section-title"><i class="bi bi-clock me-1"></i>운영 시간</div>
        ${isWeekdayUnknown && isHolidayUnknown
            ? `<small class="text-muted">* 운영 시간은 확인 후 이용 바랍니다.</small>`
            : `
              ${!isWeekdayUnknown ? `<div class="small text-muted">평일: <strong>${weekdayTime}</strong></div>` : ""}
              ${!isHolidayUnknown ? `<div class="small text-muted">공휴일: <strong>${holidayTime}</strong></div>` : ""}
            `
          }
      </div>

      <hr class="my-2"/>

      <!-- STATS: 주차면 -->
      <div class="parking-stats text-center small">
        <i class="bi bi-car-front-fill me-1 text-secondary"></i>
        전체 <strong>${p.capacity}</strong>면 |
        <span class="${available === 0 ? 'text-danger' : 'text-success'} fw-semibold">가능 ${available}</span>면
      </div>
    </div>
  `,
        borderWidth: 0,
        backgroundColor: 'transparent',
        disableAnchor: true,
      });

      naver.maps.Event.addListener(marker, 'click', () => {
        info.open(map, marker);
      });

      marker._infoWindow = info;
    }

    parkingMarkers.push(marker);
  });

  // ✅ 지도 클릭 시 InfoWindow 닫기 (중복 등록 방지용 플래그)
  if (!map._parkingClickBound) {
    naver.maps.Event.addListener(map, 'click', function () {
      parkingMarkers.forEach(marker => {
        if (marker._infoWindow) marker._infoWindow.close();
      });
    });
    map._parkingClickBound = true; // 한 번만 등록되도록 설정
  }
}

// ✅ API 데이터 로딩
window.loadSeoulCityParking = function () {
  fetch('/api/parking')
    .then(res => res.json())
    .then(list => {
      window.parkingListData = list;
      initRegionSelectors();
      renderParkingList(list);
      renderParkingMarkers(list);
    })
    .catch(err => {
      console.error("❌ 주차장 불러오기 실패", err);
      alert("주차장 정보를 불러올 수 없습니다.");
    });
};

// ✅ 목록 렌더링
// ✅ 주차장 목록 렌더링 함수
window.renderParkingList = function (list) {
  const box = document.getElementById("parkingListBox");
  const countText = document.getElementById("parkingCount");

  // 🔄 목록 초기화
  box.innerHTML = "";

  // ❌ 데이터 없음 처리
  if (!list.length) {
    box.innerHTML = `
      <div class="text-center text-muted py-3">
        <i class="bi bi-exclamation-circle me-1"></i> 표시할 주차장이 없습니다.
      </div>
    `;
    if (countText) countText.textContent = 0;
    return;
  }

  // ✅ 주차장 목록 생성
  list.forEach(parking => {
    const available = Math.max(0, parking.capacity - parking.currentCount);

    // 🧱 리스트 아이템 요소 생성
    const item = document.createElement("div");
    item.className = "list-group-item list-group-item-action";
    item.innerHTML = `
      <div class="d-flex justify-content-between align-items-center mb-1">
        <div class="fw-bold text-primary">${parking.name}</div>
        <span class="badge bg-${available === 0 ? 'danger' : 'success'}">
          ${available === 0 ? '만차' : '가능: ' + available + '면'}
        </span>
      </div>
      <div class="small text-muted mb-1">
        <i class="bi bi-geo-alt-fill me-1"></i> ${parking.address}
      </div>
      <div class="small text-secondary">
        <i class="bi bi-car-front-fill me-1"></i> 전체주차면: <strong>${parking.capacity}</strong>
      </div>
    `;

    // 🧭 클릭 시 지도 이동 및 마커 트리거
    item.onclick = () => {
      const marker = parkingMarkers.find(m => m.getTitle() === parking.name);
      if (marker) {
        map.panTo(marker.getPosition());
        naver.maps.Event.trigger(marker, 'click');
      }
    };

    box.appendChild(item);
  });

  // 🔢 총 개수 표시
  if (countText) countText.textContent = list.length;
};

// ✅ 필터 적용
window.applyAdvancedParkingFilters = function () {
  const gu = document.getElementById("parkingGuSelect")?.value;
  const dong = document.getElementById("parkingDongSelect")?.value;
  const payType = document.getElementById("payTypeSelect")?.value;
  const parkingType = document.getElementById("parkingTypeSelect")?.value;
  const keyword = document.getElementById("parkingSearchInput")?.value.trim().toLowerCase();

  let filtered = window.parkingListData || [];

  if (payType) filtered = filtered.filter(p => p.payType === payType);
  if (parkingType) filtered = filtered.filter(p => p.parkingType === parkingType);
  if (gu) filtered = filtered.filter(p => p.address.includes(gu));
  if (dong) filtered = filtered.filter(p => p.address.includes(dong));
  if (keyword) filtered = filtered.filter(p => p.name.toLowerCase().includes(keyword));

  renderParkingList(filtered);
  renderParkingMarkers(filtered);
};

// ✅ 구/동 리스트 추출
function extractGuDongFromList(list) {
  const guSet = new Set();
  const dongMap = {};

  list.forEach(p => {
    const parts = p.address.trim().split(" ");
    const gu = parts.find(word => word.endsWith("구"));
    const dong = parts.find(word => /(동|로|가)$/.test(word));
    if (!gu || !dong) return;

    guSet.add(gu);
    if (!dongMap[gu]) dongMap[gu] = new Set();
    dongMap[gu].add(dong);
  });

  return {
    guList: Array.from(guSet).sort(),
    dongMap: Object.fromEntries(Object.entries(dongMap).map(([k, v]) => [k, Array.from(v).sort()])),
  };
}

// ✅ 셀렉터 초기화
window.initRegionSelectors = function () {
  const { guList, dongMap } = extractGuDongFromList(window.parkingListData || []);
  const guSelect = document.getElementById('parkingGuSelect');
  const dongSelect = document.getElementById('parkingDongSelect');

  guSelect.innerHTML = '<option value="">전체 구</option>';
  dongSelect.innerHTML = '<option value="">전체 동</option>';

  guList.forEach(gu => {
    guSelect.innerHTML += `<option value="${gu}">${gu}</option>`;
  });

  function handleGuChange() {
    const selectedGu = guSelect.value;

    dongSelect.innerHTML = '<option value="">전체 동</option>';
    if (dongMap[selectedGu]) {
      dongMap[selectedGu].forEach(dong => {
        dongSelect.innerHTML += `<option value="${dong}">${dong}</option>`;
      });
    }

    if (guCenterMap[selectedGu]) {
      const center = guCenterMap[selectedGu];
      const latLng = new naver.maps.LatLng(center.lat, center.lng);
      if (window.INITIAL_ZOOM != null) map.setZoom(window.INITIAL_ZOOM);
      map.panTo(latLng);
    }

    filterParkingByRegion();
  }

  guSelect.onchange = handleGuChange;
  guSelect.onclick = handleGuChange;
  dongSelect.onchange = filterParkingByRegion;
};

// ✅ 필터링 함수
window.filterParkingByRegion = function () {
  const gu = document.getElementById('parkingGuSelect').value;
  const dong = document.getElementById('parkingDongSelect').value;
  const keyword = document.getElementById('parkingSearchInput').value.trim().toLowerCase();
  const onlyAvailable = document.getElementById('onlyAvailableCheckbox')?.checked;

  let filtered = window.parkingListData || [];

  if (gu) filtered = filtered.filter(p => p.address.includes(gu));
  if (dong) filtered = filtered.filter(p => p.address.includes(dong));
  if (keyword) filtered = filtered.filter(p => p.name.toLowerCase().includes(keyword));
  if (onlyAvailable) filtered = filtered.filter(p => (p.capacity - p.currentCount) > 0);

  renderParkingList(filtered);
  renderParkingMarkers(filtered);
};

// 이벤트 등록 및 함수 정의를 한 번에
(function () {
  const input = document.getElementById('parkingSearchInput');

  // 엔터 키 이벤트 바인딩
  input.addEventListener('keydown', function (event) {
    if (event.key === 'Enter') {
      event.preventDefault();
      doSearch();
    }
  });

  // 버튼 클릭용 함수도 동일 함수 사용
  window.searchParking = function () {
    doSearch();
  };

  // 실제 검색 실행 함수
  function doSearch() {
    filterParkingByRegion();
  }
})();

window.resetParkingPanel = function () {
  document.getElementById('parkingSearchInput').value = '';
  document.getElementById('parkingGuSelect').value = '';
  document.getElementById('parkingDongSelect').value = '';
  document.getElementById('payTypeSelect').value = '';
  document.getElementById('parkingTypeSelect').value = '';
  document.getElementById('onlyAvailableCheckbox').checked = false;
  filterParkingByRegion();
};
