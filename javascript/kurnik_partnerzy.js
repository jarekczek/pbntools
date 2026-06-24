// ==UserScript==
// @name         Kurnik partnerzy
// @namespace    http://tampermonkey.net/
// @version      0.1
// @description  try to take over the world!
// @author       You
// @match        https://www.kurnik.pl/stat.phtml?u=*&g=br&sk=3*
// @match        https://www.kurnik.pl/stat.phtml?u=*&g=br&sk=4*
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
    var curPageNr = 1
    if (window.location.href.indexOf("page=") >= 0) {
        curPageNr = Number(window.location.href.replace(/.*page=/, ""))
    }

    var a = document.createElement("input")
    a.type = "button"
    a.value = "fetchNext"
    a.onclick = function() { fetchResultsFromPage(curPageNr += 1) }
    addElemToPage(a)

    collectScoresFromDocument(document.body)
    displayScoreSummary(curPageNr)

    function collectScoresFromDocument(doc) {
        var rows = doc.querySelectorAll("tr")
        rows.forEach(row => {
            var cols = row.querySelectorAll("td")
            if (cols.length != 0) {
                var score = Number(cols[3].innerText)
                var scoreCnt = Number(cols[2].innerText)
                sum += scoreCnt * score
                cnt += scoreCnt
            }
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
    }

    function cumulatePlayersScore(names, score) {
        if (playersCnts[names] == null) {
            playersCnts[names] = 0
            playersSums[names] = 0
        }
        playersCnts[names] += 1
        playersSums[names] += score
    }

    function displayScoreSummary(pageNr) {
        addTextToPageAsFirst("page " + pageNr + ", cnt: " + cnt + ", sum: " + sum.toFixed(2) + ", avg: " + (sum/cnt).toFixed(2))
    }

    function addTextToPage(textToAdd) {
        var p = document.createElement("p")
        p.innerText = textToAdd
        addElemToPage(p)
    }

    function addTextToPageAsFirst(textToAdd) {
        var p = document.createElement("p")
        p.innerText = textToAdd
        addElemToPageAsFirst(p)
    }

    function addElemToPage(elemToAdd) {
        var baseElem = document.querySelectorAll("form")[1]
        var div = document.createElement("div")
        div.id = "divTamper"
        div.appendChild(elemToAdd)
        baseElem.insertAdjacentElement("beforeBegin", div)
    }

    function addElemToPageAsFirst(elemToAdd) {
        var addedDivs = document.querySelectorAll("#divTamper")
        if (addedDivs.length <= 1) {
            addElemToPage(elemToAdd)
        } else {
            var baseElem = addedDivs[1]
            var div = document.createElement("div")
            div.id = "divTamper"
            div.appendChild(elemToAdd)
            baseElem.insertAdjacentElement("beforeBegin", div)
        }
    }

    function fetchResultsFromPage(pageNr) {
        var link = window.location.href
        if (link.indexOf("page=") < 0) {
            link += "&page=" + pageNr
        }
        var newLink = link.replace(/page=([0-9]+)$/, function(m, p1) { return "page=" + (pageNr) })
        console.log("newLink: " + newLink)
        var xhr = new XMLHttpRequest();
        xhr.open("GET", newLink, true);
        xhr.onreadystatechange = function () {
            if(xhr.readyState === 4 && xhr.status === 200) {
                var html = document.createElement("HTML")
                html.innerHTML = xhr.responseText
                collectScoresFromDocument(html)
                displayScoreSummary(pageNr)
            }
        };
        xhr.send();
    }

})();
