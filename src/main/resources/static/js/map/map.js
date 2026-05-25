document.getElementById("currentDate").textContent = new Date().toLocaleDateString("pt-BR", {
    weekday: "long", year: "numeric", month: "long", day: "numeric"
});

const urlParams = new URLSearchParams(window.location.search);
let busId = urlParams.get("busId");

if (!token) {
    window.location.href = "/auth/login";
}

if (busId) {
    document.getElementById("subscribeSection").style.display = "none";
    document.getElementById("trackingSection").style.display = "";
    startLocationTracking(busId, updateMap);
}

document.getElementById("btnSubscribe").addEventListener("click", async function () {
    const sign = document.getElementById("signInput").value.trim().toUpperCase();
    const errorEl = document.getElementById("subscribeError");

    if (!sign) {
        errorEl.textContent = "Digite a placa do ônibus";
        errorEl.style.display = "";
        return;
    }

    errorEl.style.display = "none";

    try {
        const res = await fetch(`${API_URL}api/passenger/${userId}/subscribe`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": "Bearer " + token
            },
            body: JSON.stringify({ sign: sign })
        });

        if (!res.ok) {
            const errText = await res.text();
            throw new Error(errText || "Erro ao assinar");
        }

        const data = await res.json();
        window.location.href = "/map?busId=" + data.idBus;
    } catch (err) {
        errorEl.textContent = "Ônibus não encontrado com esta placa";
        errorEl.style.display = "";
    }
});
