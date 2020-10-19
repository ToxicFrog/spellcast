let $ = sel => document.querySelector(sel);
let $$ = sel => document.querySelectorAll(sel);

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

function showGesturePicker(picker) {
  picker.classList.remove("hidden");
}

function initGesturePicker(player, hand) {
  let cell = $('#gesture-'+player+'-'+hand+'-0');
  let picker = $('#gesture-picker-'+hand);
  cell.onclick = _ => showGesturePicker(picker);
  picker.style.left = cell.x + 'px';
  picker.style.top = cell.y + 'px';
}

function initSpellcast(player) {
  initGesturePicker(player, "left");
  initGesturePicker(player, "right");
  refresh('/log', 0, $('#log'), renderLog);
  let talk = $("#talk");
  talk.addEventListener(
    'change', function(event) {
      post('/game/log', {"text": event.target.value});
      event.target.value = '';
    })
}
