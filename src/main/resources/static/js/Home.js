function getCookie(name) {
    const match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
    return match ? match[2] : null;
}

const token = getCookie("token");

if (!token) {
    window.location.href = "/auth/login";
}

fetch("/home", {
    headers: {
        "Authorization": "Bearer " + token
    }
})
    .then(res => res.json())
    .then(data => {
        console.log(data);
    });
