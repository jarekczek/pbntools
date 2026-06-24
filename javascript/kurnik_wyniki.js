// ==UserScript==
// @name         Kurnik wyniki
// @namespace    http://tampermonkey.net/
// @version      0.1
// @description  try to take over the world!
// @author       You
// @match        https://www.kurnik.pl/stat.phtml*
// @icon         https://www.google.com/s2/favicons?domain=kurnik.pl
// @grant        none
// ==/UserScript==

(function() {
    'use strict';

    var cnt = 0
    var sum = 0
    var sum4 = 0
    var values = []
    var playersCnts = {}
    var playersSums = {}

    var lastH = document.querySelectorAll("th")[5]
    var newH = document.createElement("th")
    newH.innerText = 'śr. 4 + wsz.'
    lastH.insertAdjacentElement("afterEnd", newH)
    var rows = document.querySelectorAll("tr")
    rows.forEach(row => {
        var cols = row.querySelectorAll("td")
        var bold = row.querySelector("b")
        if (bold != null) {
            //console.log(row.innerText)
            cnt += 1
            var val = Number(bold.innerText)
            values.push(val)
            sum += val
            sum4 += val
            if (cnt >= 5) {
                sum4 -= values[cnt - 5]
            }
            //console.log("" + cnt +  " " + sum4 + ", " + values[cnt - 5])
            var nextCol = bold.parentElement.nextElementSibling
            var newCol = document.createElement("td")
            newCol.innerText = "" + (sum4/4).toFixed(2) + " " + (sum/cnt).toFixed(2)
            nextCol.insertAdjacentElement("afterEnd", newCol)
            var players = bold.parentElement.parentElement.children[1].innerText
            cumulatePlayersScore(players, val)
        }
    })
    displayScoreSummary()

    function cumulatePlayersScore(names, score) {
        if (playersCnts[names] == null) {
            playersCnts[names] = 0
            playersSums[names] = 0
        }
        playersCnts[names] += 1
        playersSums[names] += score
    }

    function displayScoreSummary() {
        var h3 = document.querySelector("h3")
        var div = document.createElement("div")
        for (var names in playersCnts) {
            var p = document.createElement("p")
            p.innerText = names + ": " + (playersSums[names] / playersCnts[names]).toFixed(2)
            div.appendChild(p)
        }
        h3.insertAdjacentElement("beforeBegin", div)
    }
})();
