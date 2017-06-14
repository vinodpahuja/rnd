//console.log("Hello from My Cool Chrome extension!")
var name = $(".profile-rail-card__actor-link").text().trim();
console.log("Hello " + name);

function enhance() {
	$(this).append($(this).children().first().clone());
};

$('.feed-s-social-action-bar').each(enhance);

var observer = new MutationObserver(function(mutrecs) {
	mutrecs.forEach(function(mutrec){
		mutrec.addedNodes.forEach(function(addedNode){
			$(addedNode).find('.feed-s-social-action-bar').each(enhance);
			console.log(addedNode);		
		});
	});
});

observer.observe($('.core-rail > div')[3], {
	childList : true
});