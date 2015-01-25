(function() {
    "use strict";

    // Declaration & Initializing
    var width, height, hideLangageList, largeHeader, language, oContent, aContactItems, oGlassContent, skipper, oHeaderBar, aImgItems, canvas, ctx, circles, target, animateHeader = true,
        isBgrAnimating = false,
        isSkipperAnimating = false,
        iCurrentBgr = 0,
        bgrInterval, noscroll = true,
        isRevealed = false,
        aAnimIn, t,
        map, pointarray, heatmap,
        infoWindows = [],
        oCounter, cnt = 0,
        oSearchField, url, oWikiSearchData, oAutocompleteList, aListEnries, oGoBtn,
        iSelectedSuggest = -1,
        geochart;

    // Bootstrap
    window.addEventListener('DOMContentLoaded', function(e) {
        initHeader();
        initContent();
        addListeners();
        // initWebSocket();
        setTimeout(function() {
            window.scrollTo(0, 0);
        }, 1);
    }, false);

    // Event handling
    function addListeners() {
        window.addEventListener('resize', resize);
        window.addEventListener('scroll', scroll);
        oSearchField.oninput = autocomplete;
        oSearchField.onkeyup = navigateSuggestions;
        // oSearchField.onfocus = autocomplete(true);
        oSearchField.onblur = hideAutocomplete;
        // oSearchField.onchange = autocomplete;
        for (var i = aListEnries.length - 1; i >= 0; i--) {
            aListEnries[i].addEventListener('click', selectWikiArticle);
        }
        oGoBtn.addEventListener('click', function() {
            hideAutocomplete();
            toggle(1);
        });
        if (!language) return;
        for (var i = 0; i < language.children.length; i++) {
            language.children[i].addEventListener('click', function(e) {
                if (e.currentTarget.nodeName === 'A') {
                    languageDialog();
                } else {
                    selectLanguage(e);
                }
            });
        }
    }

    function initHeader() {
        width = window.innerWidth;
        height = window.innerHeight;
        target = {
            x: 0,
            y: height
        };

        largeHeader = document.getElementById('large-header');
        largeHeader.style.height = height + 'px';

        canvas = document.getElementById('header-canvas');
        canvas.width = width;
        canvas.height = height;
        ctx = canvas.getContext('2d');

        // create particles
        circles = [];
        var numberCircles = 100; //width * 0.5;
        for (var x = 0; x < numberCircles; x++) {
            var c = new Circle();
            circles.push(c);
        }
        limitLoop(animateHeaderCanvas, 60);

        aImgItems = document.querySelector('ul.image-wrap').children;
        bgrInterval = setInterval(animateBackgroundTransition, 15000);

        aContactItems = document.querySelectorAll('.btn-wrapper');

        var i = 0;
        var timeoutAnimation = function() {
            setTimeout(function() {
                if (aContactItems.length == 0) return;
                aContactItems[i].className = aContactItems[i].className + ' fxFlipInX';
                i++;
                if (i < aContactItems.length) timeoutAnimation();
            }, 100);
        }

        disable_scroll();
        timeoutAnimation();
    }

    // function initWebSocket() {
    //     var socket = io('localhost:3000');

    //     socket.on('newCamera', function(data){
    //         console.log(data);

    //         var image = 'img/mapcamNew.svg';
    //         addCamMarker(data, image, true);
    //     });
    // }

    function initContent() {
        oContent = document.getElementById('content');
        oHeaderBar = document.querySelector('header');
        oGlassContent = document.querySelector("#glass-content");
        oCounter = document.querySelector(".number");
        // skipper = document.querySelector("#header-skipper");
        aAnimIn = document.querySelectorAll('.animation-init');
        language = document.getElementById('language-setting');
        oSearchField = document.querySelector(".wiki-search");
        oAutocompleteList = document.querySelector(".autocomplete");
        aListEnries = oAutocompleteList.querySelectorAll("li");
        var iTranslateX = 50;
        for (var i = aListEnries.length - 1; i >= 0; i--) {
            iTranslateX = iTranslateX * -1;
            aListEnries[i].style.transform = "translateX(" + iTranslateX + "%)";
        }
        oGoBtn = document.querySelector(".go");

        oContent.style.height = window.innerHeight + 'px';
        oContent.height = window.innerHeight + 'px';

        // GOOGLE MAP CHARTS API
        google.setOnLoadCallback(function() {
        //     var data = google.visualization.arrayToDataTable([
        //         ['Country', 'Edits'],
        //         ['Germany', 200],
        //         ['United States', 300],
        //         ['Brazil', 400],
        //         ['Canada', 500],
        //         ['France', 600],
        //         ['RU', 700]
        //     ]);

        //     var options = {
        //         // region: 'IT',
        //         // region: '155', // Western Europe
        //         // displayMode: 'markers',
        //         colorAxis: {
        //             colors: ['red'],
        //             minValue: 0
        //         },
        //         // backgroundColor: '#81d4fa',
        //         // datalessRegionColor: '#f8bbd0'
        //     };
            geochart = new google.visualization.GeoChart(document.getElementById('map'));
        //     geochart.draw(data, options);
        });

        // GOOGLE MAPS API
        // Map Style made with http://gmaps-samples-v3.googlecode.com/svn/trunk/styledmaps/wizard/index.html
        // var mapStyle = [{
        //     "featureType": "water",
        //     "elementType": "labels",
        //     "stylers": [{
        //         "visibility": "off"
        //     }]
        // }, {
        //     "featureType": "transit",
        //     "stylers": [{
        //         "visibility": "off"
        //     }]
        // }, {
        //     "featureType": "road",
        //     "stylers": [{
        //         "visibility": "off"
        //     }]
        // }, {
        //     "featureType": "poi",
        //     "stylers": [{
        //         "visibility": "off"
        //     }]
        // }, {
        //     "featureType": "landscape",
        //     "stylers": [{
        //         "visibility": "simplified"
        //     }, {
        //         "color": "#ffffff"
        //     }]
        // }, {
        //     "featureType": "administrative.land_parcel",
        //     "stylers": [{
        //         "visibility": "off"
        //     }]
        // }, {
        //     "featureType": "administrative.neighborhood",
        //     "stylers": [{
        //         "visibility": "off"
        //     }]
        // }, {
        //     "featureType": "administrative.province",
        //     "stylers": [{
        //         "visibility": "off"
        //     }]
        // }, {
        //     "featureType": "administrative.locality",
        //     "elementType": "labels",
        //     "stylers": [{
        //         "visibility": "off"
        //     }]
        // }, {
        //     "featureType": "administrative.country",
        //     "elementType": "geometry",
        //     "stylers": [{
        //         "color": "#cccccc"
        //     }]
        // }, {
        //     "featureType": "administrative.country",
        //     "elementType": "labels.text.fill",
        //     "stylers": [{
        //         "color": "#555555"
        //     }]
        // }];

        // // Create a new StyledMapType object
        // var styledMap = new google.maps.StyledMapType(mapStyle, {
        //     name: "WikiWars Map"
        // });

        // var berlin = new google.maps.LatLng(52.31, 13.42);

        // var map = new google.maps.Map(document.getElementById('map'), {
        //     center: berlin,
        //     zoom: 3,
        //     mapTypeControlOptions: {
        //         mapTypeIds: [google.maps.MapTypeId.ROADMAP, 'map_style']
        //     },
        //     mapTypeControl: false,
        //     streetViewControl: false
        // });

        // map.mapTypes.set('map_style', styledMap);
        // map.setMapTypeId('map_style');

        // var world_geometry = new google.maps.FusionTablesLayer({
        //     query: {
        //         select: 'geometry',
        //         from: '1N2LBk4JHwWpOY4d9fobIn27lfnZ5MDy-NoqqRpk'
        //     },
        //     styles: [{
        //         markerOptions: {
        //             iconName: "large_green"
        //         },
        //         polygonOptions: {
        //             fillColor: '#ffffff',
        //             strokeColor: "#ffffff",
        //             strokeWeight: 0,
        //             fillOpacity: 0
        //         },
        //         polylineOptions: {
        //             strokeColor: "#ffffff",
        //             strokeWeight: 0
        //         }
        //     }, {
        //         where: "ISO_2DIGIT IN ('FR', 'VA', 'RU')",
        //         polygonOptions: {
        //             fillColor: '#ff0000',
        //             strokeColor: "#ff0000",
        //             strokeWeight: 0.5,
        //             fillOpacity: 0.9
        //         }
        //     }, {
        //         where: "ISO_2DIGIT IN ('US', 'GB', 'DE')",
        //         polygonOptions: {
        //             fillColor: '#ff3300',
        //             strokeColor: "#ff3300",
        //             strokeWeight: 0.5,
        //             fillOpacity: 0.5
        //         }
        //     }],
        //     map: map,
        //     suppressInfoWindows: true
        // });

        // LEAFLET API
        // var map = L.map('map').setView([52.31, 13.42], 3); // Berlin as start point
        // L.tileLayer('https://{s}.tiles.mapbox.com/v3/{id}/{z}/{x}/{y}.png', {
        //     maxZoom: 18,
        //     minZoom: 3,
        //     attribution: 'Footer',
        //     // 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, ' +
        //     // '<a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, ' +
        //     // 'Imagery © <a href="http://mapbox.com">Mapbox</a>',
        //     id: 'examples.map-20v6611k',
        //     detectRetina: true
        //     // maxBounds:  [[30.0,-85.0],[50.0,-65.0]]
        // }).addTo(map);
    }

    function hideAutocomplete() {
        var iTranslateX = -50;
        for (var i = 0; i < aListEnries.length; i++) {
            iTranslateX = iTranslateX * -1;
            aListEnries[i].style.transform = "translateX(" + iTranslateX + "%)";
            aListEnries[i].style.pointerEvents = "none";
            aListEnries[i].style.opacity = 0;
            aListEnries[i].visible = false;
        }
    }

    function autocomplete(suppressValidation) {
        var sSearchText = oSearchField.value;
        if (suppressValidation != true) {
            oSearchField.className = oSearchField.className.replace(" valid", "");
            oGoBtn.className = oGoBtn.className.replace(" display", "");
        }
        if (iSelectedSuggest >= 0) {
            aListEnries[iSelectedSuggest].className = "";
            iSelectedSuggest = -1;
        }

        // Input validation
        sSearchText = sSearchText.replace(/[%?=&]/ig, ""); ///\W/gi, "");
        oSearchField.value = sSearchText;

        // AJAX Test
        var r = new XMLHttpRequest();
        r.open("GET", "/revisions/suggest?search=" + sSearchText + "&limit=4", true);
        r.responseType = "json";
        r.onreadystatechange = function() {
            if (r.readyState != 4 || r.status != 200) return;
            var iTranslateX = -50;
            // if (r.response[1].length >= 1) {
            for (var i = 0; i < aListEnries.length; i++) {
                var sSuggestedTerm = r.response[1][i + 1];
                if (sSuggestedTerm != undefined) {
                    aListEnries[i].innerHTML = "<span class='highlight'>" +
                        sSuggestedTerm.slice(0, sSearchText.length) +
                        "</span>" +
                        sSuggestedTerm.slice(sSearchText.length, sSuggestedTerm.length);
                    aListEnries[i].setAttribute("data-article", sSuggestedTerm);
                }

                if (sSuggestedTerm != undefined) {
                    aListEnries[i].style.transform = "translateX(0)";
                    aListEnries[i].style.pointerEvents = "auto";
                    aListEnries[i].style.opacity = 1;
                    aListEnries[i].visible = true;
                } else {
                    iTranslateX = iTranslateX * -1;
                    aListEnries[i].style.transform = "translateX(" + iTranslateX + "%)";
                    aListEnries[i].style.pointerEvents = "none";
                    aListEnries[i].style.opacity = 0;
                    aListEnries[i].visible = false;
                }
                if (sSuggestedTerm == sSearchText && sSuggestedTerm != undefined) {
                    oSearchField.className += " valid";
                    oGoBtn.className += " display";
                }
            }
        }
        r.send();
    }

    function selectWikiArticle(e, articleName) {
        if (!articleName) {
            var e = e || window.event;
            var oWikiListItem = e.currentTarget;
            var sArticleName = oWikiListItem.getAttribute("data-article");
        } else {
            sArticleName = articleName;
        }

        // Clear Classes
        oSearchField.className = oSearchField.className.replace(" valid", "");
        oGoBtn.className = oGoBtn.className.replace(" display", "");

        // Set Classes
        oSearchField.value = sArticleName;
        oSearchField.className += " valid";
        oGoBtn.className += " display";
        autocomplete(true);
    }

    function navigateSuggestions(e) {
        var e = e || window.event;
        var iAvailableEntries;
        for (var i = aListEnries.length - 1; i >= 0; i--) {
            if (aListEnries[i].style.opacity == 1) {
                iAvailableEntries = i + 1;
                break;
            }
        }
        switch (e.keyCode) {
            case 33: // Page Up
            case 38: // Arrow Up
                // case 37: // Arrow Left
                e.preventDefault();
                if (iSelectedSuggest < 0) {
                    iSelectedSuggest = iAvailableEntries - 1;
                    aListEnries[iSelectedSuggest].className = "selected";
                } else {
                    aListEnries[iSelectedSuggest].className = "";
                    iSelectedSuggest = (iSelectedSuggest + iAvailableEntries - 1) % iAvailableEntries;
                    aListEnries[iSelectedSuggest].className = "selected";
                }
                break;
            case 9: // Tab
                // Is not working, yet
            case 34: // Page Down
                // case 39: // Arrow Right
            case 40: // Arrow Down
                if (iSelectedSuggest < 0) {
                    iSelectedSuggest = 0;
                    aListEnries[iSelectedSuggest].className = "selected";
                } else {
                    aListEnries[iSelectedSuggest].className = "";
                    iSelectedSuggest = (iSelectedSuggest + 1) % iAvailableEntries;
                    aListEnries[iSelectedSuggest].className = "selected";
                }
                break;
            case 13: // ENTER
                if (iSelectedSuggest == -1 && oSearchField.classList.contains("valid")) {
                    toggle(1);
                } else if (iAvailableEntries > 0 && iSelectedSuggest >= 0) {
                    e.preventDefault();
                    aListEnries[iSelectedSuggest].className = "";
                    e.currentTarget = aListEnries[iSelectedSuggest];
                    selectWikiArticle(e, aListEnries[iSelectedSuggest].getAttribute("data-article"));
                    iSelectedSuggest = -1;
                }
                hideAutocomplete();
                break;
            default:
                return;
        };
    }

    function languageDialog() {
        if (language.className === '') {
            language.className = 'language-select';
            clearTimeout(hideLangageList);
            hideLangageList = setTimeout(languageDialog, 4000);
        } else {
            language.className = '';
        }
    }

    function selectLanguage(e) {
        if (e.currentTarget.classList.contains('language-selected')) {
            e.preventDefault();
        } else {
            e.currentTarget.parentNode.querySelector('.language-selected').className = '';
            e.currentTarget.className = 'language-selected';
            var currentLanguage = e.currentTarget.getAttribute('data-language');
        };
        clearTimeout(hideLangageList);
        hideLangageList = setTimeout(languageDialog, 4000);
    }

    function disable_scroll() {
        var preventDefault = function(e) {
            e = e || window.event;
            if (e.preventDefault)
                e.preventDefault();
            e.returnValue = false;
        };
        document.body.ontouchmove = function(e) {
            preventDefault(e);
        };
    }

    function enable_scroll() {
        window.onmousewheel = document.onmousewheel = document.onkeydown = document.body.ontouchmove = null;
    }

    function scroll() {
        // Update Glass header
        // var iOffset = window.pageYOffset;
        // if (oGlassContent) oGlassContent.style.marginTop = (iOffset * -1) + 'px';

        // Upate Large Header
        var scrollVal = window.pageYOffset || largeHeader.scrollTop;
        if (noscroll) {
            if (scrollVal < 0) return false;
            window.scrollTo(0, 0);
        }
        if (isSkipperAnimating) {
            return false;
        }
        if (scrollVal <= 0 && isRevealed) {
            // toggle(0);
        } else if (scrollVal > 0 && !isRevealed) {
            // toggle(1);
        }

        // Animate Sections
        for (var i = 0; i < aAnimIn.length; i++) {
            var animationElement = aAnimIn[i];

            var docViewTop = window.pageYOffset;
            var docViewBottom = docViewTop + window.innerHeight;

            var elemTop = animationElement.offsetTop;
            // var elemBottom = elemTop + animationElement.offsetHeight;

            if (elemTop <= docViewBottom - animationElement.offsetHeight / 3) {
                if (animationElement.classList.contains('animate-in') == false) {
                    animationElement.className = animationElement.className + ' animate-in';
                }
            }
        }
    }

    function resize() {
        width = window.innerWidth;
        height = window.innerHeight;
        largeHeader.style.height = height + 'px';
        canvas.width = width;
        canvas.height = height;

        oContent.style.height = height + 'px';
        oContent.height = height + 'px';
    }

    function toggle(reveal) {

        console.log(oSearchField.value);

        var timescope = "6m";
        var aggregation = "m";

        // Execute Query
        var r = new XMLHttpRequest();
        // ToDo handle non-URL characters in article
        r.open("GET", "/revisions/analyse/"+oSearchField.value+
                        "?timescope="+timescope+
                        "&aggregation="+aggregation, true);
        r.responseType = "json";
        r.onreadystatechange = function() {
            if (r.readyState != 4 || r.status != 200) return;
            console.log(r.status);
            var countries = new Array();

            console.log(r.response.revisions);

            if (r.response.revisions) {
                countries.push(['Country', 'Edits']);
                for (var j = 0; j < r.response.revisions[0].summary.length; j++) {
                    if (r.response.revisions[0].summary[j].country != "") {
                        countries.push([
                            r.response.revisions[0].summary[j].country,
                            r.response.revisions[0].summary[j].editSize
                            ]);
                    }
                }
                console.log(countries);
                var geodata = google.visualization.arrayToDataTable(countries);
                geochart.draw(geodata);
            }
        }
        r.send();

        isSkipperAnimating = true;

        if (reveal) {
            document.body.className = 'revealed';
        } else {
            animateHeader = true;
            noscroll = true;
            disable_scroll();
            document.body.className = '';
            // Reset animated content
            for (var i = 0; i < aAnimIn.length; i++) {
                var animationElement = aAnimIn[i];
                animationElement.className = animationElement.className.replace(' animate-in', '');
            }
        }

        // simulating the end of the transition:
        setTimeout(function() {
            isRevealed = !isRevealed;
            isSkipperAnimating = false;
            if (reveal) {
                animateHeader = false;
                noscroll = false;
                enable_scroll();
            }
        }, 1200);
    }

    function animateBackgroundTransition(dir) {
        if (isBgrAnimating) return false;
        isBgrAnimating = true;
        var cntAnims = 0;
        var itemsCount = aImgItems.length;
        dir = dir || 'prev';

        var currentItem = aImgItems[iCurrentBgr];
        if (dir === 'next') {
            iCurrentBgr = (iCurrentBgr + 1) % itemsCount;
        } else if (dir === 'prev') {
            iCurrentBgr = (itemsCount + iCurrentBgr - 1) % itemsCount;
            // console.log(iCurrentBgr);
        }
        var nextItem = aImgItems[iCurrentBgr];

        var classAnimIn = dir === 'next' ? 'navInNext' : 'navInPrev'
        var classAnimOut = dir === 'next' ? 'navOutNext' : 'navOutPrev';
        // console.log(dir);

        var onEndAnimationCurrentItem = function() {
            currentItem.className = '';
            ++cntAnims;
            if (cntAnims === 2) {
                isBgrAnimating = false;
            }
        }

        var onEndAnimationNextItem = function() {
            nextItem.className = 'current';
            ++cntAnims;
            if (cntAnims === 2) {
                isBgrAnimating = false;
            }
        }

        setTimeout(onEndAnimationCurrentItem, 2000);
        setTimeout(onEndAnimationNextItem, 2000);

        currentItem.className = currentItem.className + ' ' + classAnimOut;
        nextItem.className = nextItem.className + classAnimIn;
    }

    var animateHeaderCanvas = function() {
        if (animateHeader) {
            ctx.clearRect(0, 0, width, height);
            for (var i in circles) {
                circles[i].draw();
            }
        }
    }

    var limitLoop = function(fn, fps) {

        // Use var then = Date.now(); if you
        // don't care about targetting < IE9
        var then = new Date().getTime();

        // custom fps, otherwise fallback to 60
        fps = fps || 60;
        var interval = 1000 / fps;

        return (function loop(time) {
            requestAnimationFrame(loop);

            // again, Date.now() if it's available
            var now = new Date().getTime();
            var delta = now - then;

            if (delta > interval) {
                // Update time
                // now - (delta % interval) is an improvement over just 
                // using then = now, which can end up lowering overall fps
                then = now - (delta % interval);

                // call the fn
                fn();
            }
        }(0));
    };

    // Canvas manipulation
    function Circle() {
        var _this = this;

        // constructor
        (function() {
            _this.pos = {};
            init();
        })();

        function init() {
            _this.pos.x = Math.random() * width;
            _this.pos.y = height + Math.random() * 100;
            _this.alpha = 0.1 + Math.random() * 0.3;
            _this.scale = 0.1 + Math.random() * 0.3;
            _this.velocity = Math.random();
        }

        this.draw = function() {
            if (_this.alpha <= 0) {
                init();
            }
            _this.pos.y -= _this.velocity;
            _this.alpha -= 0.0005;
            ctx.beginPath();
            ctx.arc(_this.pos.x, _this.pos.y, _this.scale * 10, 0, 2 * Math.PI, false);
            ctx.fillStyle = 'rgba(255,255,255,' + _this.alpha + ')';
            ctx.fill();
        };
    }
})();
