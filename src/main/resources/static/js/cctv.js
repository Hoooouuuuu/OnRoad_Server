window.ITS_API_KEY;

let allCctvDataITS = [], allCctvDataEX = [], cctvMarkers = [];
let hls = null;
let currentVideoUrl = '', currentCctvType = 'ex';
let sortAscending = true, isDataLoaded = false;
let exList = [], itsList = [];

function cleanCctvName(raw) {
  return raw.replace(/\[[^\]]*\]/g, '').trim();
}

function extractRoadFromCctv(cctvname) {
  const match = cctvname.match(/\[(.*?)\]/);
  if (!match) return null;
  let name = match[1].replace(/\s/g, '');
  const suffixExceptions = ['창선', '환선', '지선', '산선', '성선', '양선', '포선'];
  return (name.endsWith('선') && !suffixExceptions.some(sfx => name.endsWith(sfx)))
    ? name.replace(/선$/, '고속도로') : name;
}

function findCctvMatch(name, roadName) {
  const cleaned = cleanCctvName(name);
  const candidates = [...allCctvDataITS, ...allCctvDataEX].filter(d => cleanCctvName(d.cctvname) === cleaned);
  if (!candidates.length) return null;
  const normalized = roadName.replace(/\s/g, '');
  return candidates.find(d => {
    const ex = extractRoadFromCctv(d.cctvname);
    return ex && normalized.includes(ex);
  }) || candidates.find(d => d.cctvname.replace(/\s/g, '').includes(normalized)) || candidates[0];
}

function fetchWithRetry(url, retries = 3) {
  return fetch(url, { credentials: 'omit' }).catch(err => {
    if (retries > 0) return fetchWithRetry(url, retries - 1);
    throw err;
  });
}

function loadCctvJsonList() {
  return Promise.all([
    fetch('/json/ex_list.json').then(r => r.json()),
    fetch('/json/its_list.json').then(r => r.json())
  ]).then(([ex, its]) => { exList = ex; itsList = its; });
}

function preloadAllCctvs() {
  document.getElementById('loadingSpinner').style.display = 'block';
  const base = 'https://openapi.its.go.kr:9443/cctvInfo';
  const buildUrl = type => `${base}?apiKey=${window.ITS_API_KEY}&type=${type}&cctvType=1&minX=124.6&maxX=132.0&minY=33.0&maxY=39.0&getType=json`;

  Promise.all([
    fetchWithRetry(buildUrl('its')).then(r => r.json()),
    fetchWithRetry(buildUrl('ex')).then(r => r.json()),
    loadCctvJsonList()
  ]).then(async ([itsRes, exRes]) => {
    allCctvDataITS = itsRes.response?.data || [];
    allCctvDataEX = exRes.response?.data || [];
    isDataLoaded = true;
    await loadRoadList();
    document.getElementById('loadingSpinner').style.display = 'none';
  }).catch(err => {
    console.error('❌ CCTV 로드 실패:', err);
    document.getElementById('loadingSpinner').style.display = 'none';
    alert('CCTV 데이터를 불러오지 못했습니다.');
  });
}

function generateRoadIconBase64(type, number) {
  const canvas = document.createElement('canvas');
  canvas.width = type === 'ex' ? 60 : 100;
  canvas.height = 60;
  const ctx = canvas.getContext('2d');
  ctx.clearRect(0, 0, canvas.width, canvas.height);

  if (type === 'its') {
    ctx.beginPath();
    ctx.ellipse(canvas.width / 2, canvas.height / 2, 50, 20, 0, 0, Math.PI * 2);
    ctx.fillStyle = '#2166d1';
    ctx.fill();
    ctx.lineWidth = 5;
    ctx.strokeStyle = 'white';
    ctx.stroke();
  }

  return new Promise(resolve => {
    if (type === 'ex') {
      const img = new Image();
      img.src = '/image/cctv_signs/cctv_ex_signs.png';
      img.onload = () => {
        ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
        drawText(ctx, number, canvas);
        resolve(canvas.toDataURL());
      };
    } else {
      drawText(ctx, number, canvas);
      resolve(canvas.toDataURL());
    }
  });
}

function drawText(ctx, number, canvas) {
  ctx.font = 'bold 28px sans-serif';
  ctx.fillStyle = 'white';
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.fillText(number, canvas.width / 2, canvas.height / 2);
}

async function loadRoadList() {
  const list = document.getElementById('roadList');
  if (!list) return;
  list.innerHTML = '';

  const roads = currentCctvType === 'ex' ? exList : itsList;
  const sortedRoads = roads.sort((a, b) =>
    parseInt(a.road_number.replace(/\D/g, '')) - parseInt(b.road_number.replace(/\D/g, ''))
  );

  const roadElements = await Promise.all(sortedRoads.map(async road => {
    const iconUrl = await generateRoadIconBase64(currentCctvType, road.road_number);
    const li = document.createElement('li');
    li.className = 'list-group-item list-group-item-action';
    li.innerHTML = `
      <div style="display:flex; flex-direction:column;">
        <div style="display:flex; align-items:center; justify-content:space-between;">
          <div style="display:flex; align-items:center; gap:8px;">
            <img src="${iconUrl}" style="width:36px;height:36px;flex-shrink:0;" />
            <strong>${road.road_name}</strong>
          </div>
          <button class="btn btn-sm btn-outline-secondary sort-btn" style="display:none;" title="정렬 순서 변경">↕</button>
        </div>
        <div class="road-section-summary" style="font-size:0.9em; color:#666; margin-left:44px;"></div>
      </div>
    `;
    li.addEventListener('click', () => toggleSection(li, road));
    return li;
  }));

  roadElements.forEach(el => list.appendChild(el));
}

function toggleSection(li, road) {
  const sectionEl = li.nextElementSibling;
  const summary = li.querySelector('.road-section-summary');

  if (sectionEl?.classList.contains('road-section')) {
    clearCctvMarkers();
    sectionEl.remove();
    summary.textContent = '';
    li.querySelector('.sort-btn').style.display = 'none';
    return;
  }

  loadRoadCctvMarkers(road);
  summary.innerHTML = `<strong>${road.route_section}</strong>`;
  const ul = document.createElement('ul');
  ul.className = 'road-section';

  const cctvs = sortAscending ? road.cctvs : [...road.cctvs].reverse();
  cctvs.forEach(name => {
    const data = findCctvMatch(name, road.road_name);
    if (!data) return;
    const sub = document.createElement('li');
    sub.className = 'list-group-item';
    sub.textContent = name;
    sub.addEventListener('click', e => {
      e.stopPropagation();
      playVideo(data.cctvurl, data.cctvname, new naver.maps.LatLng(+data.coordy, +data.coordx));
    });
    ul.appendChild(sub);
  });

  li.insertAdjacentElement('afterend', ul);

  const sortBtn = li.querySelector('.sort-btn');
  sortBtn.style.display = 'inline-block';
  sortBtn.onclick = e => {
    e.stopPropagation();
    sortAscending = !sortAscending;
    ul.remove();
    summary.textContent = '';
    toggleSection(li, road);
  };
}

function loadRoadCctvMarkers(road) {
  clearCctvMarkers();
  const bounds = new naver.maps.LatLngBounds();

  road.cctvs?.forEach(name => {
    const data = findCctvMatch(name, road.road_name);
    if (!data || isNaN(+data.coordx) || isNaN(+data.coordy)) return;
    const position = new naver.maps.LatLng(+data.coordy, +data.coordx);
    const marker = new naver.maps.Marker({
      map,
      position,
      title: name,
      icon: {
        url: '/image/cctv-icon.png',
        size: new naver.maps.Size(44, 66),
        anchor: new naver.maps.Point(22, 22)
      }
    });
    naver.maps.Event.addListener(marker, 'click', () => playVideo(data.cctvurl, data.cctvname, position));
    cctvMarkers.push(marker);
    bounds.extend(position);
  });

  if (cctvMarkers.length) map.fitBounds(bounds);
}

function playVideo(url, name, position) {
  const container = document.getElementById('videoContainer');
  const video = document.getElementById('cctvVideo');
  const title = document.getElementById('videoTitle');

  title.textContent = name;
  currentVideoUrl = url;

  if (hls) hls.destroy();
  hls = new Hls();
  hls.loadSource(url);
  hls.attachMedia(video);
  hls.on(Hls.Events.MANIFEST_PARSED, () => video.play().catch(console.warn));

  container.style.display = 'block';
  video.style.display = 'block';
  map.panTo(position);

  const rect = document.getElementById('map').getBoundingClientRect();
  container.style.left = `${rect.left + rect.width / 2 - container.offsetWidth / 2}px`;
  container.style.top = `${rect.top + rect.height / 2 - container.offsetHeight / 2 + 130}px`;

  document.getElementById('closeVideoBtn').onclick = hideVideo;
  document.getElementById('fullscreenBtn').onclick = () => video.requestFullscreen?.();
  document.getElementById('openNewTabBtn').onclick = () => openInNewTab(currentVideoUrl, name);
  makeVideoContainerDraggable();
}

function openInNewTab(url, title) {
  const win = window.open('', '_blank', 'width=800,height=600');
  win.document.write(`
    <html><head><title>${title}</title>
    <script src="https://cdn.jsdelivr.net/npm/hls.js@latest"></script>
    <style>body{margin:0;background:#000;}video{width:100%;height:100vh;object-fit:contain;}</style>
    </head><body>
    <video id="video" controls autoplay muted></video>
    <script>
      const video = document.getElementById('video');
      if (Hls.isSupported()) {
        const hls2 = new Hls();
        hls2.loadSource('${url}');
        hls2.attachMedia(video);
        hls2.on(Hls.Events.MANIFEST_PARSED, ()=>video.play());
      } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
        video.src='${url}'; video.play();
      }
    </script></body></html>`);
}

function hideVideo() {
  if (hls) hls.destroy();
  hls = null;
  document.getElementById('videoContainer').style.display = 'none';
}

function clearCctvMarkers() {
  cctvMarkers.forEach(m => m.setMap(null));
  cctvMarkers = [];
}

function makeVideoContainerDraggable() {
  const container = document.getElementById('videoContainer');
  const header = container.querySelector('.video-header');
  let dragging = false, offsetX = 0, offsetY = 0;

  container.style.position = 'absolute';
  header.style.cursor = 'grab';

  header.addEventListener('mousedown', e => {
    if (e.clientY - header.getBoundingClientRect().top > header.offsetHeight / 2) return;
    dragging = true;
    const rect = container.getBoundingClientRect();
    offsetX = e.clientX - rect.left;
    offsetY = e.clientY - rect.top;
    header.style.cursor = 'grabbing';
    e.preventDefault();
  });

  document.addEventListener('mousemove', e => {
    if (!dragging) return;
    const nx = Math.min(Math.max(0, e.clientX - offsetX), window.innerWidth - container.offsetWidth);
    const ny = Math.min(Math.max(0, e.clientY - offsetY), window.innerHeight - container.offsetHeight);
    requestAnimationFrame(() => {
      container.style.left = `${nx}px`;
      container.style.top = `${ny}px`;
    });
  });

  document.addEventListener('mouseup', () => {
    dragging = false;
    header.style.cursor = 'grab';
  });
}

// 초기화
document.addEventListener('DOMContentLoaded', () => {
  document.getElementById('highway')?.addEventListener('click', () => {
    currentCctvType = 'ex';
    loadRoadList();
  });

  document.getElementById('normalroad')?.addEventListener('click', () => {
    currentCctvType = 'its';
    loadRoadList();
  });

  document.getElementById('tabHighway')?.addEventListener('click', () => {
    currentCctvType = 'ex';
    clearCctvMarkers();
    loadRoadList();
  });

  document.getElementById('tabNormalroad')?.addEventListener('click', () => {
    currentCctvType = 'its';
    clearCctvMarkers();
    loadRoadList();
  });

  preloadAllCctvs();
});

// 전역 등록
window.loadRoadList = loadRoadList;
window.clearCctvMarkers = clearCctvMarkers;
window.hideVideo = hideVideo;