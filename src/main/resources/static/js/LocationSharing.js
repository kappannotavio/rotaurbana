let driverId = null;
let selectedBusId = null;
let selectedRouteId = null;
let tracking = false;
let lastSend = 0;

function getCookie(name) {
    const match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
    return match ? match[2] : null;
}

const token = getCookie("token");
driverId = getCookie("driverId");

if (!token || !driverId || driverId === "" || driverId === "null") {
    window.location.href = "/auth/login";
}

const busSelect = document.getElementById("busSelect");
const routeSelect = document.getElementById("routeSelect");
const routeInfo = document.getElementById("routeInfo");
const codeDisplay = document.getElementById("codeDisplay");
const statusText = document.getElementById("statusText");
const statusIndicator = document.getElementById("statusIndicator");

async function loadBuses() {
    busSelect.innerHTML = '<option value="">Carregando...</option>';
    try {
        const res = await fetch("/api/driver/" + driverId + "/buses", {
            headers: { "Authorization": "Bearer " + token }
        });
        if (!res.ok) throw new Error("Erro " + res.status);
        const buses = await res.json();
        busSelect.innerHTML = '<option value="">Selecione um ônibus</option>';
        buses.forEach(b => {
            const opt = document.createElement("option");
            opt.value = b.idBus;
            opt.textContent = b.sign + " (" + b.code + ")";
            busSelect.appendChild(opt);
        });
    } catch (err) {
        busSelect.innerHTML = '<option value="">Erro ao carregar ônibus</option>';
    }
}

async function loadRoutes(busId) {
    routeSelect.disabled = true;
    routeSelect.innerHTML = '<option value="">Carregando...</option>';
    try {
        const res = await fetch("/api/driver/" + driverId + "/buses/" + busId + "/routes", {
            headers: { "Authorization": "Bearer " + token }
        });
        if (!res.ok) throw new Error("Erro " + res.status);
        const routes = await res.json();
        routeSelect.innerHTML = '<option value="">Selecione uma rota</option>';
        routes.forEach(r => {
            const opt = document.createElement("option");
            opt.value = r.idRoute;
            opt.textContent = r.destiny + " (" + r.code + ")";
            opt.dataset.code = r.code;
            routeSelect.appendChild(opt);
        });
        routeSelect.disabled = false;
    } catch (err) {
        routeSelect.innerHTML = '<option value="">Erro ao carregar rotas</option>';
        routeSelect.disabled = false;
    }
}

busSelect.addEventListener("change", function() {
    selectedBusId = this.value;
    selectedRouteId = null;
    codeDisplay.textContent = "------";
    routeInfo.textContent = "";
    if (selectedBusId) {
        loadRoutes(selectedBusId);
    } else {
        routeSelect.innerHTML = '<option value="">Primeiro selecione um ônibus</option>';
        routeSelect.disabled = true;
    }
});

routeSelect.addEventListener("change", function() {
    selectedRouteId = this.value;
    if (selectedRouteId) {
        const opt = this.options[this.selectedIndex];
        codeDisplay.textContent = opt.dataset.code || "------";
        routeInfo.textContent = opt.textContent;
        startTracking();
    } else {
        codeDisplay.textContent = "------";
        routeInfo.textContent = "";
    }
});

function startTracking() {
    if (!selectedBusId || !selectedRouteId) return;
    tracking = true;
    statusText.textContent = "Compartilhando localização...";
    statusIndicator.className = "status-dot active";
}

function sendToBackend(lat, lon) {
    if (!tracking || !selectedBusId || !selectedRouteId) return;

    fetch("/api/driver/" + driverId + "/tracking/" + selectedBusId, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "Authorization": "Bearer " + token
        },
        body: JSON.stringify({ latitude: lat, longitude: lon })
    }).catch(err => console.error("Erro ao enviar:", err));

    fetch("/api/driver/" + driverId + "/routes/" + selectedRouteId + "/tracking", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "Authorization": "Bearer " + token
        },
        body: JSON.stringify({ latitude: lat, longitude: lon })
    }).catch(err => console.error("Erro ao enviar rota:", err));
}

if ("geolocation" in navigator) {
    navigator.geolocation.watchPosition(function(pos) {
        const now = Date.now();
        if (now - lastSend < 1000) return;
        lastSend = now;
        sendToBackend(pos.coords.latitude, pos.coords.longitude);
    }, function(err) {
        console.error("Erro ao obter localização:", err);
    }, {
        enableHighAccuracy: true,
        timeout: 1000,
        maximumAge: 0
    });
}

const params = new URLSearchParams(window.location.search);
const initialBusId = params.get("busId");
const initialRouteId = params.get("routeId");

loadBuses().then(() => {
    if (initialBusId) {
        busSelect.value = initialBusId;
        busSelect.dispatchEvent(new Event("change"));
        if (initialRouteId) {
            setTimeout(() => {
                routeSelect.value = initialRouteId;
                routeSelect.dispatchEvent(new Event("change"));
            }, 500);
        }
    }
});
