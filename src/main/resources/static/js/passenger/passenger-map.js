(function() {
    var token, userId;
    try {
        token = (document.cookie.match(/(^| )token=([^;]+)/) || [])[2] || null;
        userId = (document.cookie.match(/(^| )userId=([^;]+)/) || [])[2] || null;
    } catch(e) {}
    if (!token || !userId) { window.location.href = '/auth/login'; return; }

    var routeId = new URLSearchParams(window.location.search).get('routeId');

    var map = null, routeLine = null, busMarker = null;
    var lastUpdate = 0;
    var pollTimer = null;
    var staleShowing = false;
    var trajetoCoords = [];
    var trajetoLine = null;
    var followBus = true;

    if (typeof L === 'undefined') {
        document.getElementById('overlayMessage').textContent = 'Erro ao carregar mapa.';
        document.getElementById('statusBar').textContent = 'Falha';
        return;
    }

    document.getElementById('statusBar').textContent = 'Iniciando mapa...';

    try {
        map = L.map('mapid', { zoomControl: false, attributionControl: false }).setView([-29.6, -54.0], 13);
        L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(map);
        L.control.zoom({ position: 'bottomright' }).addTo(map);
        document.getElementById('statusBar').textContent = 'Mapa carregado';
    } catch(e) { return; }

    function busIcon() {
        return L.divIcon({
            html: '<div style="background:#ff5722;width:20px;height:20px;border-radius:50%;border:3px solid #fff;box-shadow:0 2px 8px rgba(0,0,0,.5);display:flex;align-items:center;justify-content:center;"><svg width="10" height="10" viewBox="0 0 24 24" style="fill:#fff"><path d="M4 16c0 .88.39 1.67 1 2.22V20c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-1h8v1c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-1.78c.61-.55 1-1.34 1-2.22V6c0-3.5-3.58-4-8-4s-8 .5-8 4v10zm3.5 1c-.83 0-1.5-.67-1.5-1.5S6.67 14 7.5 14s1.5.67 1.5 1.5S8.33 17 7.5 17zm9 0c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zm-10-5V6h11v6h-11z"/></svg></div>',
            iconSize: [20, 20], iconAnchor: [10, 10], className: ''
        });
    }

    function makeIcon(color) {
        return L.divIcon({
            html: '<div style="background:' + color + ';width:18px;height:18px;border-radius:50%;border:3px solid #fff;box-shadow:0 2px 6px rgba(0,0,0,.3);"></div>',
            iconSize: [18, 18], iconAnchor: [9, 9], className: ''
        });
    }

    function desenharRota() {
        if (depLat == null || depLng == null || destLat == null || destLng == null) return;
        var url = 'https://router.project-osrm.org/route/v1/driving/' + depLng + ',' + depLat + ';' + destLng + ',' + destLat + '?geometries=geojson&overview=full';
        fetch(url).then(function(r) { return r.json(); }).then(function(data) {
            if (data.code !== 'Ok' || !data.routes || !data.routes[0]) return;
            var coords = data.routes[0].geometry.coordinates;
            var latlngs = coords.map(function(c) { return [c[1], c[0]]; });
            if (routeLine) map.removeLayer(routeLine);
            routeLine = L.polyline(latlngs, { color: '#0088ff', weight: 4, opacity: 0.7 }).addTo(map);
        }).catch(function() {});
    }

    if (depLat != null && depLng != null && destLat != null && destLng != null) {
        L.marker([depLat, depLng], { icon: makeIcon('#00d28a') }).addTo(map).bindPopup('Embarque');
        L.marker([destLat, destLng], { icon: makeIcon('#ff5722') }).addTo(map).bindPopup('Desembarque');

        var pts = [];
        pts.push([depLat, depLng]);
        pts.push([destLat, destLng]);
        routeLine = L.polyline(pts, {
            color: '#0088ff', weight: 3, dashArray: '8, 10', opacity: 0.6
        }).addTo(map);

        if (depLat !== destLat || depLng !== destLng) {
            map.fitBounds(pts, { padding: [40, 40] });
        }
        desenharRota();
    }

    function hideOverlay() { var el = document.getElementById('mapOverlay'); if (el) el.style.display = 'none'; }
    function showOverlay(msg) { var el = document.getElementById('mapOverlay'); var msgEl = document.getElementById('overlayMessage'); if (el) el.style.display = 'flex'; if (msgEl) msgEl.textContent = msg; }

    setTimeout(function() {
        if (lastUpdate === 0) {
            hideOverlay();
            document.getElementById('statusBar').textContent = 'Aguardando sinal do motorista...';
        }
    }, 5000);

    function fetchLocation() {
        if (!routeId || !map) return;
        if (navigator.onLine === false) { showOverlay('Sem conexao com a internet'); return; }
        fetch('/api/passenger/' + userId + '/tracking-by-route/' + routeId, {
            headers: { 'Authorization': 'Bearer ' + token }
        }).then(function(r) {
            if (r.status === 204 || r.status === 404) return null;
            if (!r.ok) throw new Error('status ' + r.status);
            return r.json();
        }).then(function(data) {
            if (!data) return;
            var lat = Number(data.latitude);
            var lng = Number(data.longitude);
            if (!isFinite(lat) || !isFinite(lng) || (lat === 0 && lng === 0)) return;
            lastUpdate = Date.now();
            staleShowing = false;
            hideOverlay();
            document.getElementById('statusBar').textContent = 'Atualizado: ' + lat.toFixed(5) + ', ' + lng.toFixed(5);
            if (busMarker) {
                busMarker.setLatLng([lat, lng]);
            } else {
                busMarker = L.marker([lat, lng], { icon: busIcon() }).addTo(map).bindPopup('Onibus');
            }
            trajetoCoords.push([lat, lng]);
            if (trajetoLine) {
                trajetoLine.setLatLngs(trajetoCoords);
            } else {
                trajetoLine = L.polyline(trajetoCoords, { color: '#ff5722', weight: 3, opacity: 0.8 }).addTo(map);
            }
            if (followBus) {
                map.panTo([lat, lng], { animate: true, duration: 0.5 });
            }
        }).catch(function() {});
    }

    function checkStale() {
        if (lastUpdate === 0) return;
        if (!staleShowing && Date.now() - lastUpdate > 7000) {
            staleShowing = true;
            showOverlay('Sinal do motorista perdido');
            document.getElementById('statusBar').textContent = 'Sinal perdido';
        }
    }

    if (routeId) {
        document.getElementById('statusBar').textContent = 'Buscando motorista...';
        pollTimer = setInterval(fetchLocation, 3000);
        setInterval(checkStale, 3000);
        setTimeout(fetchLocation, 500);
    } else {
        document.getElementById('overlayMessage').textContent = 'Nenhuma rota selecionada';
        document.getElementById('statusBar').textContent = 'Aguardando rota';
    }

    window.addEventListener('beforeunload', function() { if (pollTimer) clearInterval(pollTimer); });

    var btnFollow = document.getElementById('btnFollow');
    if (btnFollow) {
        btnFollow.addEventListener('click', function() {
            followBus = !followBus;
            btnFollow.classList.toggle('active');
            if (followBus && busMarker) {
                map.panTo(busMarker.getLatLng(), { animate: true, duration: 0.3 });
            }
        });
    }
    map.on('dragstart', function() {
        if (followBus) {
            followBus = false;
            if (btnFollow) btnFollow.classList.remove('active');
        }
    });
})();
