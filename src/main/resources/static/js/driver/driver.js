function getCookie(name) {
    const match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
    return match ? match[2] : null;
}

const token = getCookie("token");
const userId = getCookie("userId");
const driverId = getCookie("driverId");

if (!token || !driverId || driverId === "" || driverId === "null") {
    window.location.href = "/auth/login";
}

document.getElementById("driverInfo").textContent = "ID do motorista: " + driverId;

async function loadBuses() {
    document.getElementById("loadingView").style.display = "block";
    document.getElementById("busesView").style.display = "none";

    try {
        const res = await fetch("/api/driver/" + driverId + "/buses", {
            headers: { "Authorization": "Bearer " + token }
        });

        if (!res.ok) throw new Error("Erro " + res.status);

        const buses = await res.json();
        document.getElementById("loadingView").style.display = "none";
        document.getElementById("busesView").style.display = "block";

        const container = document.getElementById("busesList");
        container.innerHTML = "";

        if (buses.length === 0) {
            container.innerHTML = '<div class="col-12 text-center text-muted-custom">Nenhum ônibus atribuído a você</div>';
            return;
        }

        buses.forEach(b => {
            const col = document.createElement("div");
            col.className = "col-md-6";
            col.innerHTML = `
                <div class="bus-card" onclick="loadRoutes(${b.idBus}, '${b.sign || b.code}')">
                    <div class="d-flex justify-content-between align-items-start">
                        <div>
                            <h6 class="mb-1">${b.sign || 'Sem placa'}</h6>
                            <small class="text-muted-custom">${b.brand || ''} ${b.model || ''}</small>
                        </div>
                        <span class="badge bg-secondary">${b.code || ''}</span>
                    </div>
                    <div class="mt-2">
                        <small class="text-muted-custom">${b.color || ''}</small>
                    </div>
                </div>
            `;
            container.appendChild(col);
        });
    } catch (err) {
        document.getElementById("loadingView").innerHTML = `
            <p class="text-danger">Erro ao carregar ônibus: ${err.message}</p>
            <button class="btn btn-orange" onclick="loadBuses()">Tentar novamente</button>
        `;
    }
}

async function loadRoutes(busId, busName) {
    document.getElementById("busesView").style.display = "none";
    document.getElementById("routesView").style.display = "block";
    document.getElementById("selectedBusName").textContent = busName;
    document.getElementById("routesList").innerHTML = '<div class="col-12 text-center text-muted-custom"><div class="spinner"></div><p class="mt-2">Carregando rotas...</p></div>';

    try {
        const res = await fetch("/api/driver/" + driverId + "/buses/" + busId + "/routes", {
            headers: { "Authorization": "Bearer " + token }
        });

        if (!res.ok) throw new Error("Erro " + res.status);

        const routes = await res.json();
        const container = document.getElementById("routesList");
        container.innerHTML = "";

        if (routes.length === 0) {
            container.innerHTML = '<div class="col-12 text-center text-muted-custom">Nenhuma rota para este ônibus</div>';
            return;
        }

        routes.forEach(r => {
            const col = document.createElement("div");
            col.className = "col-md-6";
            col.innerHTML = `
                <div class="bus-card" onclick="startTracking(${busId}, ${r.idRoute})">
                    <h6 class="mb-1">${r.destiny || 'Sem destino'}</h6>
                    <small class="text-muted-custom">
                        ${r.departureTime || '--:--'} → ${r.destinationTime || '--:--'}
                    </small>
                    <div class="mt-1">
                        <span class="badge bg-secondary">${r.code || ''}</span>
                    </div>
                </div>
            `;
            container.appendChild(col);
        });
    } catch (err) {
        document.getElementById("routesList").innerHTML = `
            <div class="col-12 text-center">
                <p class="text-danger">Erro ao carregar rotas: ${err.message}</p>
                <button class="btn btn-orange" onclick="backToBuses()">Voltar</button>
            </div>
        `;
    }
}

function backToBuses() {
    document.getElementById("routesView").style.display = "none";
    document.getElementById("busesView").style.display = "block";
}

function startTracking(busId, routeId) {
    window.location.href = "/sendLocation?busId=" + busId + "&routeId=" + routeId;
}

loadBuses();
