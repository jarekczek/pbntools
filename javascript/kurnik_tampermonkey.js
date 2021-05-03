// ==UserScript==
// @name         kurnik
// @namespace    http://tampermonkey.net/
// @version      0.1
// @description  try to take over the world!
// @author       You
// @match        https://www.kurnik.pl/brydz/
// @match        file:///J:/brydz/kurnik/kurnik2.html
// @icon         https://www.google.com/s2/favicons?domain=kurnik.pl
// @grant        none
// ==/UserScript==

(function() {
    'use strict';

    // .tlst.usno - lista wszystkich graczy
    // .tbvusers.usno - chyba też wszyscy

    window.kurnik = {}
    window.kurnik.players = {}
    var players = window.kurnik.players
    if (window.location.href.indexOf("file") >= 0) {
        window.kurnik.intervalHandle = window.setInterval(function() { scheduledFunction() }, 2000)
    } else {
        window.kurnik.intervalHandle = window.setInterval(function() { scheduledFunction() }, 2000)
    }
    modifyStylesheet()
    setupSettings()

    function modifyStylesheet() {
        document.styleSheets[0].insertRule('.jctooltip { background-color: yellow; }')
    }

    function setupSettings() {
        var tampermonkeyCheckbox = Array.from(document.querySelectorAll("input")).filter(e => e.nextSibling != null && e.nextSibling.textContent == "tampermonkey")[0]
        if (tampermonkeyCheckbox != null) {
            return
        }
        var soundsCheckbox = Array.from(document.querySelectorAll("input")).filter(e => e.nextSibling != null && e.nextSibling.textContent == "dźwięki")[0]
        if (soundsCheckbox == null) {
            console.log("sounds checkbox not present")
            return
        }

        var div = document.createElement("DIV")
        var input = document.createElement("INPUT")
        input.type = "checkbox"
        input.checked = true
        div.appendChild(input)
        div.appendChild(document.createTextNode("tampermonkey"))
        soundsCheckbox.parentElement.insertAdjacentElement('afterend', div)
        window.kurnik.enabledCheckbox = input
        console.log("tampermonkey checkbox inserted")
    }

    function scheduledFunction() {
        setupSettings()
        if (window.kurnik.enabledCheckbox != null && window.kurnik.enabledCheckbox.checked) {
            updatePlayersObecni(); watchChat();
        } else {
            //console.log("tampermonkey is disabled")
        }
    }

    function updatePlayersObecni() {
        document.querySelectorAll(".ul.uls1 .ulnm").forEach(e => {
            var matcher = /^([^(]+)(\(([a-z]+)\))?$/.exec(e.textContent)
            if (matcher == null) {
                console.log("failed player regex on " + e.textContent)
            } else {
                var playerName = matcher[1]
                addPlayerMaybe(playerName)
                players[playerName].country = matcher[3]
            }
        })
    }

    function watchChat() {
        //console.log("players:")
        //console.log(players)
        gatherPlayersFromChat()
        document.querySelectorAll(".mb1s.bsbb .tind").forEach(e => {
            var chatText = e.innerText
            for (const playerName in players) {
                //console.log("testing " + playerName + " against " + chatText)
                if (chatText.indexOf(playerName) >= 0) {
                    //console.log("chat with player " + playerName + ": " + chatText)
                }
            }
        })
    }

    function gatherPlayersFromChat() {
        // Chat typu: + przychodzi 01paula [-0.75]
        document.querySelectorAll(".mb1s.bsbb .tind").forEach(e => {
            var chatText = e.innerText
            if (chatText.indexOf("+ przychodzi") >= 0) {
                var matcher = /\+ przychodzi ([^ ]+)( \[([^\]]+)\])?/.exec(chatText)
                if (matcher == null) {
                    console.log("regex przychodzi failed for " + chatText)
                } else {
                    var playerName = matcher[1]
                    addPlayerMaybe(playerName)
                    players[playerName].score = matcher[3]
                    attachPlayerInfo(e, players[playerName])
                }
            }
        })

        // Ktoś coś mówi, to jest w boldzie.
        document.querySelectorAll(".mb1s.bsbb .tind b").forEach(e => {
            var playerName = e.innerText
            //console.log("player from chat: " + playerName)
            addPlayerMaybe(playerName)
            attachPlayerInfo(e, players[playerName])
        })
    }

    function addPlayerMaybe(playerName) {
        if (!(playerName in players)) {
            console.log("new player: " + playerName)
            var player = createPlayer(playerName)
            players[playerName] = player
            fetchPlayerStats(player)
        }
    }

    function createPlayer(playerName) {
        var player = {}
        player.name = playerName
        player.score = '?'
        player.unfinished = '?'
        return player
    }

    function attachPlayerInfo(elem, player) {
        while (elem.nextElementSibling != null && elem.nextElementSibling.classList.contains("jctooltip")) {
            elem.parentElement.removeChild(elem.nextElementSibling)
        }

        var span = document.createElement("SPAN")
        span.appendChild(document.createTextNode(" [" + player.score + "] "))
        span.classList.add('jctooltip')
        var statsLink = document.createElement("A")
        statsLink.href = "https://www.kurnik.pl/stat.phtml?u=" + player.name + "&g=br"
        statsLink.innerText = "st"
        statsLink.target = "_blank"
        span.appendChild(statsLink)
        span.appendChild(document.createTextNode(" "))
        var handsLink = document.createElement("A")
        handsLink.href = "https://www.kurnik.pl/stat.phtml?u=" + player.name + "&g=br&sk=2"
        handsLink.innerText = "ha"
        handsLink.target = "_blank"
        span.appendChild(handsLink)
        if (player.unfinished != '0%') {
            var unf = document.createElement("SPAN")
            unf.style = "background-color: red"
            unf.appendChild(document.createTextNode(player.unfinished))
            span.appendChild(document.createTextNode(" "))
            span.appendChild(unf)
        }
        elem.insertAdjacentElement('afterend', span)
        elem.classList.add('jctooltipOwner')
    }

    function fetchPlayerStats(player) {
        var xhr = new XMLHttpRequest();
        var url = "https://www.kurnik.pl/stat.phtml?u=" + player.name + "&g=br";
        xhr.open("GET", url, true);
        xhr.onreadystatechange = function () {
            if(xhr.readyState === 4 && xhr.status === 200) {
                var html = document.createElement("HTML")
                html.innerHTML = xhr.responseText
                var allP = Array.from(html.querySelectorAll(".clcol")[0].querySelectorAll("p"))
                var nieukonczone = allP
                    .filter(p => p.textContent.indexOf("nieuko") >= 0)
                    .map(p => p.querySelector("b").textContent)
                player.unfinished = nieukonczone
            }
        };
        xhr.send();
        console.log("started")
    }
})();
