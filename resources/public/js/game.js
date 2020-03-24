function refreshLog() {
  fetch("/game/log", {credentials: "same-origin"})
  .then(function(response) {
    let log = document.getElementById('log');
    if (!response.ok) {
      log.innerHTML = 'Error reading message log -- reload the page.';
      setTimeout(refreshLog, 10000);
      return;
    }
    response.text().then(function(body) {
      document.getElementById("log").innerHTML = body;
      setTimeout(refreshLog, 1000);
    });
  })
}

function post(url, body) {
  return fetch(url, {
    credentials: "same-origin",
    method: 'POST',
    body: body,
  })
}

function initSpellcast() {
  refreshLog();
  let talk = document.getElementById("talk");
  talk.addEventListener(
    'change', function(event) {
      post('/game/log', event.target.value);
      event.target.value = '';
    })
}
