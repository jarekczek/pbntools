var pw=null;

function hv_popup(handid)
{
	hv_popup_internal(handid,0,null);
}
function hv_popuplin(movie)
{
	hv_popup_internal(movie,1,null);
}
function hv_popupvug(vugid)
{
	hv_popup_internal(0,0,vugid);
}


function hv_popup_internal(handidormovie,ismovie,isvugid)
{

//pw=null;
if ( pw )
{
	if ( !pw.closed )
	{
		if ( ismovie )
		{
			pw.location = 'http://www.bridgebase.com/tools/handviewer.html?lin='+handidormovie ;

		}
		else if ( isvugid) 
		{
  		pw.location = 'http://www.bridgebase.com/tools/handviewer.html?linurl=http://www.bridgebase.com/tools/vugraph_linfetch.php?id='+isvugid ;
		}
		else
		{
			pw.location = 'http://www.bridgebase.com/tools/handviewer.html?myhand='+handidormovie ;
		}
		pw.focus();
	}
	else
	{
		pw = null;
	}
}

if ( !pw )
{

var maxx= 0.6;
var maxy= 0.6;
var ar1x = 0.75;


var w = screen.width * maxx ;
var h = screen.height * maxy ;


var aw;
var ah;


if ( w * ar1x > h )
{
	ah = h;
	aw = h / ar1x;
	
}
else
{
	aw = w;
	ah = w * ar1x;
}

aw = Math.floor(aw);
ah = Math.floor(ah);

var l = (screen.width - aw) / 2;
var t = (screen.height - ah) / 2;

var randomnumber=Math.floor(Math.random()*10000);

if (ismovie)
{
		newurl = 'http://www.bridgebase.com/tools/handviewer.html?lin='+handidormovie ;

}
else if ( isvugid) 
{
  		newurl = 'http://www.bridgebase.com/tools/handviewer.html?linurl=http://www.bridgebase.com/tools/vugraph_linfetch.php?id='+isvugid ;
}
else
{
		newurl = 'http://www.bridgebase.com/tools/handviewer.html?myhand='+handidormovie ;
}
pw = window.open(newurl,'bboHVa1'+randomnumber,'toolbar=no, location=no, directories=no, status=no, menubar=no, scrollbars=no, resizable=yes, width='+ aw +  ', height=' + ah + ',left=' + l + ',top=' + t);

} 


}


