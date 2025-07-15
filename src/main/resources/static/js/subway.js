let subwayLayerVisible = false;
let subwayMarkers = [];
let stationMarkers = [];
let subwayRefreshInterval = null;

// ✅ 기본 지하철 노선들 (체크박스용 유지)
const subwayLines = [
  "1호선", "2호선", "3호선", "4호선", "5호선",
  "6호선", "7호선", "8호선", "9호선",
  "수인분당선", "신분당선", "경의중앙선", "경춘선", "공항철도",
  "서해선", "김포골드라인"
];

// ✅ 지하철 SVG 초기화
window.initSubwaySvgView = function (forceReload = false) {
  const container = document.getElementById("svgContainer");
  const svgView = document.getElementById("svgView");
  const mapEl = document.getElementById("map");

  if (!container || !svgView) {
    console.warn("❌ SVG 컨테이너가 없습니다.");
    return;
  }

  const svgAlreadyLoaded = container.querySelector("svg");
  const needsReload = forceReload || !svgAlreadyLoaded;

  if (!needsReload) {
    svgView.style.display = 'block';
    if (mapEl) mapEl.style.display = 'none';
    return;
  }

  fetch('/svg/seoul-subway.svg')
    .then(response => {
      if (!response.ok) throw new Error("🚨 SVG 로딩 실패");
      return response.text();
    })
    .then(svgText => {
      container.innerHTML = svgText;
      container.dataset.loaded = 'true';

      svgView.style.display = 'block';
      if (mapEl) mapEl.style.display = 'none';

      const svgEl = container.querySelector("svg");
      if (svgEl) {
        svgEl.style.width = "100%";
        svgEl.style.height = "100%";
        svgEl.style.display = "block";
      }

      enableSvgPanZoom();
      console.log("✅ SVG 로딩 성공");
    })
    .catch(err => {
      console.error("❌ SVG fetch 오류:", err);
    });
};

// ✅ 지도 복귀
function showMap() {
  const mapEl = document.getElementById("map");
  const svgView = document.getElementById("svgView");

  if (mapEl) mapEl.style.display = "block";
  if (svgView) svgView.style.display = "none";
}

// ✅ SVG Pan/Zoom
function enableSvgPanZoom() {
  const container = document.getElementById("svgContainer");
  const svg = container.querySelector("svg");
  if (!svg) return;

  let g = svg.querySelector("g");
  if (!g) {
    g = document.createElementNS("http://www.w3.org/2000/svg", "g");
    while (svg.firstChild) g.appendChild(svg.firstChild);
    svg.appendChild(g);
  }

  let isPanning = false;
  let startX = 0, startY = 0;
  let translateX = 0, translateY = 0;
  let scale = 1;

  function updateTransform() {
    g.setAttribute("transform", `translate(${translateX}, ${translateY}) scale(${scale})`);
  }

  svg.addEventListener("mousedown", (e) => {
    isPanning = true;
    startX = e.clientX;
    startY = e.clientY;
    svg.style.cursor = "grabbing";
  });

  svg.addEventListener("mousemove", (e) => {
    if (!isPanning) return;
    const dx = e.clientX - startX;
    const dy = e.clientY - startY;
    translateX += dx;
    translateY += dy;
    startX = e.clientX;
    startY = e.clientY;
    updateTransform();
  });

  svg.addEventListener("mouseup", () => {
    isPanning = false;
    svg.style.cursor = "grab";
  });

  svg.addEventListener("mouseleave", () => {
    isPanning = false;
    svg.style.cursor = "grab";
  });

  svg.addEventListener("wheel", (e) => {
    e.preventDefault();
    const delta = e.deltaY > 0 ? -0.1 : 0.1;
    const newScale = scale + delta;
    if (newScale < 0.3 || newScale > 4) return;
    scale = newScale;
    updateTransform();
  });

  svg.style.cursor = "grab";
  updateTransform();
}

// ✅ 지하철 버튼 클릭 콘솔 디버그 (선택)
document.addEventListener("DOMContentLoaded", () => {
  const btn = document.getElementById("sidebarSubwayBtn");
  if (btn) {
    btn.addEventListener("click", () => {
      console.log("🟢 지하철 사이드바 버튼 클릭됨");
    });
  }
});