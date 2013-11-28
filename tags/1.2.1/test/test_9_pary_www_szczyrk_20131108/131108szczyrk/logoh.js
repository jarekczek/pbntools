function loadlogoh(id, fn)
{
 advAJAX.get({
    url: fn,
    unique: true,
    onSuccess : function(obj) 
              { 
                document.getElementById(id).innerHTML=obj.responseText;
              },
    onError : function(obj) {
        alert(id+":"+fn+" error: " + obj.status);
    }

});

}


function loadlogohdf()
{
  loadlogoh('logoh', './logoh/logoh.inc.html');
}