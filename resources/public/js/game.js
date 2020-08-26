function refresh(url, stamp, element, render) {
  let path = url + "/" + stamp;
  fetch(path, {credentials: "same-origin"})
  .then(function(response) {
    if (!response.ok) {
      element.innerHTML = 'Error loading ' + path + ' -- try reloading the page.';
      return;
    }
    response.json().then(function(json) {
      element.innerHTML = render(json);
      setTimeout(refresh, 1, url, json.stamp, element, render);
    });
  })
}

function renderLog(json) {
  return json.log.join('<br/>');
}

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
  refresh('/log', 0, document.getElementById('log'), renderLog);
  let talk = document.getElementById("talk");
  talk.addEventListener(
    'change', function(event) {
      post('/game/log', {"text": event.target.value});
      event.target.value = '';
    })
}
