$(function() {

	function doWSCall(argument) { 


    $.getJSON( "/analyze?article="+argument, function( data ) {
	  //alert(data);
    var arrContainer = [data.revisions.length];
    
    for (var i = 0; i < data.revisions.length; i++) {

        var regions = Array();
        for (var j = 0; j < data.revisions[i].summary.length; j++) {
          if(regions[data.revisions[i].summary[j].country] == undefined ){
            regions[data.revisions[i].summary[j].country] = data.revisions[i].summary[j].frequency;
          }
        };
        var d = new Date(data.revisions[i].timeStamp);

        arrContainer[i] = {"time": d.toGMTString(),
                           "data": regions};
    }

    gdpTimeData = arrContainer;


    $('#world-map').vectorMap({
      map: 'world_en',
      backgroundColor: '#3ECC98',
      series: {
        regions: [{
          values: gdpTimeData[0].data,
          scale: ['#C8EEFF', '#0071A4'],
          normalizeFunction: 'polynomial'
        }]
      },
      onRegionLabelShow: function(e, el, code){
        //el.html(el.html()+' (GDP - '+gdpTimeData[($('#slider').slider( "value" ) - 1)].data.[code]+')');
      }
    });

    //var mapObject = $('#world-map').vectorMap('get', 'mapObject');
    $("#slider").slider({  
      //value: gdpTimeData.length - 1,     
        min: 1,
        max: gdpTimeData.length - 1,
        animate: "fast",
        step: 1,
        slide: function(e, ui) {
            updateMap(ui.value);
        }
     });

    $('#slider1Value').html(gdpTimeData[0].time);

	});
  }

	$(".panel").css({"height":$(window).height()});
	$.scrollify({
		section:".panel"
	});
	

	$(".scroll").click(function(e) {
		e.preventDefault();
		$.scrollify("move",$(this).attr("href"));
	});

	
 
	


  $( "#tags" ).autocomplete({
    source: availableTags
  });

  $( "#analyze" )
  	.button()
  	.click(function( event ) {
      //debugger;
      console.log($( "#tags" ).val());
      doWSCall($( "#tags" ).val());
  		$.scrollify("move","#map");
	});

  $( "#play" )
      .click(function( event ) {

      	//Go to first timestamp
      	//$('#slider').slider( "value", 1 );
      	//updateMap(1);

      	for (var i = 1; i < gdpTimeData.length; i++) {
      		$('#slider').slider( "value", i );
      		updateMap(i);
      		//yield sleep(2000);
      	};
        //alert("test" + val);

      });


    $('#whatever a').tagcloud({
	  size: {start: 10, end: 100, unit: 'pt'},
	  color: {start: '#cde', end: '#f52'}
	});

});


function updateMap(pValue) {
	/*var hours = Math.floor(ui.value / 60);
    var minutes = ui.value - (hours * 60);

    if(hours.length == 1) hours = '0' + hours;
    if(minutes.length == 1) minutes = '0' + minutes;
    if(minutes==0)minutes = '00';*/
    var value;
    if ((pValue + 1) == gdpTimeData.length) {
    	value = "LIVE";
    } else {
    	value = gdpTimeData[pValue-1].time;
    };
     

    $('#slider1Value').html(value);
	
	//alert();
	  var mapObject = $('#world-map').vectorMap('get', 'mapObject');
    mapObject.series.regions[0].clear();
    mapObject.series.regions[0].setValues(gdpTimeData[pValue].data);
    //mapObject = $('#world-map').vectorMap('get', 'mapObject');
}

var gdpTimeData = [{
    "time" : "2011",
    "data" : {
      "DZ" : 8.97,
      "AF" : 16.63,
      "AL" : 11.58
    }
  },{
    "time" : "2012",
    "data" : {
      "DZ" : 58.97,
      "AF" : 16.63,
      "AL" : 11.58
    }
  },{
    "time" : "2013",
    "data" : {
      "DZ" : 158.97,
      "AF" : 16.63,
      "AL" : 11.58
    }
  },{
    "time" : "2014",
    "data" : {
      "DZ" : 258.97,
      "AF" : 16.63,
      "AL" : 11.58
    }
  },{
    "time" : "2015",
    "data" : {
      "DZ" : 358.97,
      "AF" : 16.63,
      "AL" : 11.58
    }
  }
];

var gdpData = {
	"AF": 16.63,
	"AL": 11.58,
	"DZ": 158.97
};

var availableTags = [
      "Crimea Crisis",
      "Coca Cola",
      "Croatia", 
      "Cristiano Ronaldo", 
      "Cro", 
      "Critical Mass", 
      "Criminal Minds", 
      "Crimea", 
      "Crush", 
      "Cricket", 
      "Criticism"
    ];