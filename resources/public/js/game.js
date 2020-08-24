function refreshLog(stamp) {
  fetch("/log/"+stamp, {credentials: "same-origin"})
  .then(function(response) {
    let log = document.getElementById('log');
    if (!response.ok) {
      log.innerHTML = 'Error reading message log -- reload the page.';
      return;
    }
    response.json().then(function(json) {
      document.getElementById("log").innerHTML = json.log.join('<br/>');
      setTimeout(refreshLog, 1, json.stamp);
    });
  })
}

function post(url, body) {
  return fetch(url, {
    credentials: "same-origin",
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(body),
  })
}

function initSpellcast() {
  refreshLog(0);
  let talk = document.getElementById("talk");
  talk.addEventListener(
    'change', function(event) {
      post('/game/log', {"text": event.target.value});
      event.target.value = '';
    })
}
