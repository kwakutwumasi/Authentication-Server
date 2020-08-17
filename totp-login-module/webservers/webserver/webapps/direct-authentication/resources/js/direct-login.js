var directLogin = {};
directLogin.animationStack = [];
directLogin.animateOut="animate__fadeOutLeft";
directLogin.animateIn="animate__fadeInRight";
directLogin.animateDuration=0.35;
directLogin.animatePanel = function(panel_id, oncomplete){
	directLogin.animationStack.push(function() {
		$(".active_panel").addClass("animate__animated "+directLogin.animateOut)
		.on("animationend", function(){
			$(".active_panel").addClass("collapse")
			.removeClass("active_panel animate__animated "+directLogin.animateOut)
			.off("animationend");
			$(panel_id).removeClass("collapse")
			.addClass("animate__animated "+directLogin.animateIn)
			.on("animationend", function() {
				$(panel_id).removeClass("animate__animated "+directLogin.animateIn)
				.addClass("active_panel")
				.off("animationend");
				if(oncomplete){
					oncomplete();
				}
			});	
		});		
	});
}

directLogin.schid = window.setInterval(function() {
	while (directLogin.animationStack.length > 0) {
		directLogin.animationStack.shift()();
	}
},500);

$("#btn_log_in").click(function() {
	$("#btn_log_in").prop("disabled", true);
	directLogin.animatePanel("#processing_panel", function() {
		directLogin.socket = new WebSocket("ws://localhost:8080/direct-authentication/fallbacktoken/"+$("#j_username").val());
		directLogin.socket.onmessage = function(message) {
			if("fallback-required" === message.data) {
				directLogin.animatePanel("#token_panel");
			} else {
				$("#authentication_id").html(message.data);
			}
		}
		
		$.ajax({
	        type: "POST",
	        contentType: "application/x-www-form-urlencoded",
	        url: "j_security_check",
	        data: {
	            j_username: $("#j_username").val(),
	            j_password: $("#j_password").val()
	        },
	        success: function(data) {
	        	$('#btn_log_in').prop("disabled", false);
	        	if(data.lastIndexOf("<login-failed></login-failed>")==-1){
	        		window.location.reload();
	        	} else {
	            	$("#btn_log_in").prop("disabled", false);
	        		$("#error_text").html("The login failed. Please try again");
	        		directLogin.animatePanel("#login_panel");
	        	}
				if(directLogin.socket){
		    		directLogin.socket.close();
				}
	        },
	        error: function(xhr) {
	        	if(408===xhr.status){
	        		alert("The log-in has timed out. The page will reload for you to log in again");
	        		window.location.reload();
	        	} else {
	        		alert("The view has expired. The page will reload to log you in");
	        		window.location.reload();
	        	}
	        }
	    });
	});
});

$("#btn_token_login").click(function(){
	if(directLogin.socket){
		directLogin.socket.send($("#token_code").val());
	}
});

if(directLogin.animateDuration>0) {
	$(function() {
		$(".animatablepanel").css("animation-duration",""+directLogin.animateDuration+"s");
	});	
}
