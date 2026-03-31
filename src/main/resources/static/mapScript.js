let h2 = document.querySelector("h2");

let map;
let marker;

// ícone de ônibus
const busIcon = L.icon({
    iconUrl: "images/busImage.png",
    iconSize: [40,40],
    iconAnchor: [20,40],
    popupAnchor: [0,-35]
});

function success(pos){

    const lat = pos.coords.latitude;
    const lng = pos.coords.longitude;

    h2.textContent = `Latitude: ${lat.toFixed(5)} | Longitude: ${lng.toFixed(5)}`;

    if(!map){



        map = L.map("mapid").setView([lat,lng],16);

        L.tileLayer("https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png",{
            attribution:"© OpenStreetMap © CARTO",
            subdomains:"abcd",
            maxZoom:19
        }).addTo(map);

        marker = L.marker([lat,lng],{icon:busIcon})
            .addTo(map)
            .bindPopup("🚌 Ônibus está aqui")
            .openPopup();

    }else{

        marker.setLatLng([lat,lng]);
        map.flyTo([lat,lng],16);

    }

}

function error(err){
    console.error(err);
    h2.textContent = "Não foi possível obter sua localização";
}

if("geolocation" in navigator){

    navigator.geolocation.watchPosition(success,error,{
        enableHighAccuracy:true,
        timeout:5000,
        maximumAge:0
    });

}else{
    h2.textContent="Geolocalização não suportada";
}