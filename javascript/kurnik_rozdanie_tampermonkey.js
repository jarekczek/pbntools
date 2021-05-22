// ==UserScript==
// @name         Kurnik rozdanie
// @namespace    http://tampermonkey.net/
// @version      0.1
// @description  try to take over the world!
// @author       You
// @match        https://www.kurnik.pl/rozd.phtml*
// @icon         https://www.google.com/s2/favicons?domain=kurnik.pl
// @grant        none
// ==/UserScript==

(function() {
    'use strict';

    var h1 = document.querySelector("h1")
    var a = document.createElement("input")
    a.type = "button"
    a.value = "next"
    a.onclick = nextDeal

    var prev = document.createElement("input")
    prev.type = "button"
    prev.value = "prev"
    prev.onclick = prevDeal

    h1.insertAdjacentElement("afterEnd", prev)
    h1.insertAdjacentElement("afterEnd", a)

    function nextDeal() {
        var link = window.location.href
        var newLink = link.replace(/hid=([0-9]+)&/, function(m, p1) { return "hid=" + (Number(p1) + 1) + '&' })
        window.location.href = newLink
    }

    function prevDeal() {
        var link = window.location.href
        var newLink = link.replace(/hid=([0-9]+)&/, function(m, p1) { return "hid=" + (Number(p1) - 1) + '&' })
        window.location.href = newLink
    }


})();