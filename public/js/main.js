
$(function(){
    //alert($("#loginWithUser").text())
    $("#loginWithUser").click(function(){
        window.location.replace("/?userId=" + $("#userId").val())
    });
})