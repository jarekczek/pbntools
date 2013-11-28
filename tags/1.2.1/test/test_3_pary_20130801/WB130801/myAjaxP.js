// Ajax support for Pary, jfr'2008
var XHR=null;
var spanid;
var urlid;
var refr;
var tout=null;
function loadIt(){
 XHR.open("GET",urlid+"?x="+new Date().getTime(),true);
 XHR.onreadystatechange=stateChange;
 XHR.send(null);
 if(refr!=0){
  tout=setTimeout("loadIt()",refr);
 }
}
function XHRobj(){
 if(typeof XMLHttpRequest!="undefined"){
  return new XMLHttpRequest();
 }
 var xhrVersion=["Microsoft.XMLHTTP","MSXML2.XMLHttp.5.0","MSXML2.XMLHttp.4.0","MSXML2.XMLHttp.3.0","MSXML2.XMLHttp"];
 for(var i=0;i<xhrVersion.length;i++){
  try{
   var xhrObj=new ActiveXObject(xhrVersion[i]);
   return xhrObj;
  } catch(e) {}
 }
 return null;
}
function stateChange(){
 if(XHR.readyState==4){
  if(XHR.status==200){
   document.getElementById(spanid).innerHTML=XHR.responseText;
  }
 }
}
function initAjax(url,sid,refresh){
 spanid=sid;
 urlid=url;
 refr=1000*refresh;
 XHR=XHRobj();
 if(XHR!=null){
  loadIt();
 }
}
