(function() {
    "use strict";
    alert("Attention: WikiWars is still in development. A final release is planned this year. You're interested in sponsoring or cooperation: mail@jascha-quintern.de");

    // Declaration & Initializing
    var geochart,
        chartTop5,
        chartTagCloud,
        btnToggleSettings = document.getElementById('btn-toggle-settings'),
        inputArticle = document.getElementById('inputArticle'),
        btnListTimeframe = Array.prototype.slice.call(document.querySelectorAll('[data-timeframe]')),
        inputDate = document.getElementById('reference-date'),
        btnTimePicker = document.getElementById('time-picker'),
        btnStartAnalysis = document.getElementById('start-analysis'),
        jquerySLider = $('#slider'),
        outputDate = document.getElementById('output-date'),
        outputArticleName = document.getElementById('output-article'),
        outputAnymRate = document.getElementById('data-anym-rate-value'),
        outputEdits = document.getElementById('output-edits'),
        RESPONSE,
        STATE = {
            requestSettings: {
                inputIsValid: {
                    set: function(v){
                        switch(v) {
                            case true:
                                classie.remove(inputArticle.parentNode, 'has-error');
                                classie.add(inputArticle.parentNode, 'has-success');
                                classie.add(inputArticle.parentNode, 'has-feedback');
                                break;
                            case false:
                                classie.remove(inputArticle.parentNode, 'has-success');
                                classie.add(inputArticle.parentNode, 'has-error');
                                classie.add(inputArticle.parentNode, 'has-feedback');
                                break;
                            case null:
                                classie.remove(inputArticle.parentNode, 'has-success');
                                classie.remove(inputArticle.parentNode, 'has-error');
                                classie.remove(inputArticle.parentNode, 'has-feedback');
                                break;
                            default:
                                // Exception
                        }
                        STATE.requestSettings.inputIsValid._store = v;},
                    get: function(){return STATE.requestSettings.inputIsValid._store},
                    _store: null},
                inputArticleValue: {
                    set: function(v){
                        inputArticle.value = v;
                        STATE.requestSettings.inputArticleValue._store = v;},
                    get: function(){return inputArticle.value},
                    _store: ""},
                timeframe: {
                    set: function(v){
                        for (var i = btnListTimeframe.length - 1; i >= 0; i--) {
                            classie.remove(btnListTimeframe[i], 'active');
                        };
                        switch (v) {
                            case "1m":
                                classie.add(document.querySelector('[data-timeframe="1m"]'), 'active');
                                STATE.requestSettings.aggregation.set("d");
                                break;
                            case "3m":
                                classie.add(document.querySelector('[data-timeframe="3m"]'), 'active');
                                STATE.requestSettings.aggregation.set("w");
                                break;
                            case "12m":
                                classie.add(document.querySelector('[data-timeframe="12m"]'), 'active');
                                STATE.requestSettings.aggregation.set("m");
                                break;
                            default:
                                // Exception
                        }
                        STATE.requestSettings.timeframe._store = v;},
                    get: function(){return STATE.requestSettings.timeframe._store;},
                    _store: "1m"},
                aggregation: {
                    set: function(v){
                        switch(v){
                            case "d": break;
                            case "w": break;
                            case "m": break;
                            default:
                                // Exception
                        }
                       STATE.requestSettings.aggregation._store = v; 
                    },
                    get: function(){return STATE.requestSettings.aggregation._store;},
                    _store: "d"
                },
                referenceDate: {
                    set: function(v){
                       STATE.requestSettings.referenceDate._store = v; 
                    },
                    get: function(){return STATE.requestSettings.referenceDate._store;},
                    _store: null
                }
                }
            },
        CHART_T5_OPTIONS = {
            animation: { duration: 0, easing: 'inAndOut', startup: true },
            legend: { position: 'none', textStyle:{color: '#ffffff'} },
            backgroundColor: 'transparent',
            colors:['#2e3143','#393d4f','#474c63','#5d6482','#646c8c'],
            dataOpacity: 1,
            vAxis:{textStyle:{color:'#ffffff'},baselineColor:'#ffffff'},
            hAxis:{textStyle:{color:'#ffffff'}}
            // vAxis:{maxValue: 100, minValue: 100}
            // vAxis: {logScale: true}
            };

    // Bootstrap
    window.addEventListener('DOMContentLoaded', function(e) {
        init();
    }, false);

    function init() {
        addListeners();
        disableScroll();
        setTimeout(function() {
            window.scrollTo(0, 0);
        }, 1);

        // Initialize jQuery-UI Slider
        jquerySLider.slider();

        // GOOGLE MAP CHARTS API
        google.setOnLoadCallback(function() {
            // init map and draw initial chart
            geochart = new google.visualization.GeoChart(document.getElementById('map'));
            var geodata = google.visualization.arrayToDataTable([['Country', 'Edits']]);
            geochart.draw(geodata);

            // init top 5 column chart and draw initial data
            chartTop5 = new google.visualization.ColumnChart(document.getElementById('chart-top5'));
            var data = new google.visualization.arrayToDataTable([['Country', 'Edits'],['',0]]);
            chartTop5.draw(data, CHART_T5_OPTIONS);

            // init tag cloud and draw initial words
            var dataTags = new google.visualization.DataTable();
            dataTags.addColumn('string', 'Label');
            dataTags.addColumn('number', 'Value');
            chartTagCloud = new TermCloud(document.getElementById('tag-cloud'));
            chartTagCloud.draw(dataTags, null);
            });
    }

    // Event handling
    function addListeners() {
        // WINDOW LISTENERS
        //window.addEventListener('resize', onResize);
        window.addEventListener('scroll', onScroll);

        // SETTINGS PANEL LISTENERS
        btnToggleSettings.addEventListener('click', onToggleSettings);
        inputArticle.oninput = onAutocomplete;
        // inputArticle.onkeyup = navigateSuggestions;
        // inputArticle.onblur = hideAutocomplete;
        btnListTimeframe.map(function(button){
            button.addEventListener('click', onChangeTimeframe);
        });
        inputDate.oninput = onDateInput;
        // inputDate.onkeyup = navigateSuggestions;
        // inputDate.onblur = hideAutocomplete;
        btnTimePicker.addEventListener('click', onTimePicker);
        btnStartAnalysis.addEventListener('click', onStartAnalysis);

        // TIMESLIDER
        jquerySLider.on( "slide", onSlideValueChange );
    }

    // Disable Scrolling
    function disableScroll() {
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

    // Enable Scrolling
    function enableScroll() {
        window.onmousewheel = document.onmousewheel = document.onkeydown = document.body.ontouchmove = null;
    }

    // On Scroll
    function onScroll(e) {

    }

    // On Resize
    function onResize(e) {
        width = window.innerWidth;
        height = window.innerHeight;
    }

    // On toggle (show/hide) settings panel
    function onToggleSettings(e) {
        console.log("Toggle Settings");
    }

    // On user input for wikipedia article
    function onAutocomplete(e) {
        var searchInput = STATE.requestSettings.inputArticleValue.get();

        // Input validation
        searchInput = searchInput.replace(/[%?=&]/ig, ""); ///\W/gi, "");
        STATE.requestSettings.inputArticleValue.set(searchInput);

        if (searchInput != "") {
            // Submit Query
            var r = new XMLHttpRequest();
            r.open("GET", "/revisions/suggest?search=" + searchInput + "&limit=4", true);
            r.responseType = "json";
            r.onreadystatechange = function() {
                if (r.readyState != 4 || r.status != 200) return;
                // Return autocomplete entries
                var searchInputIsValid = false;
                var match;
                for (var i = 0; i < r.response[1].length; i++) {
                    var autocompleteEntry = r.response[1][i];
                    console.log(autocompleteEntry);
                    if (autocompleteEntry.toLowerCase() == searchInput.toLowerCase()) {
                        // add has-success has-feedback
                        searchInputIsValid = true;
                        match = autocompleteEntry;
                    }
                }
                // Check if there was a match
                if (searchInputIsValid) {
                    STATE.requestSettings.inputArticleValue.set(match);
                    STATE.requestSettings.inputIsValid.set(true);
                } else {
                    STATE.requestSettings.inputIsValid.set(false);
                };
            }
            r.send();
        } else {
            STATE.requestSettings.inputIsValid.set(null);
        };
    }

    // On change timeframe
    function onChangeTimeframe(e) {
        STATE.requestSettings.timeframe.set(e.currentTarget.getAttribute('data-timeframe'));
    }

    // On user input reference date
    function onDateInput(e) {
        console.log("Date User Input");
    }

    // On user called time picker
    function onTimePicker(e) {
        console.log("timepicker");
    }

    // On START ANALYSIS
    function onStartAnalysis(e) {
        // Execute Query
        var r = new XMLHttpRequest();
        var searchTerm = escape(STATE.requestSettings.inputArticleValue.get());
        var timeframe = STATE.requestSettings.timeframe.get();
        var aggregation = STATE.requestSettings.aggregation.get();
        r.open("GET", "/revisions/analyse/"+searchTerm+
                        "?timescope="+timeframe+
                        "&aggregation="+aggregation, true);
        console.log("/revisions/analyse/"+searchTerm+
                        "?timescope="+timeframe+
                        "&aggregation="+aggregation);
        r.responseType = "json";
        r.onreadystatechange = function() {
            if (r.readyState != 4 || r.status != 200) return;
            console.log(r.response);

            RESPONSE = r.response;

            jquerySLider.slider( "option", "min", 0 );
            jquerySLider.slider( "option", "max", RESPONSE.revisions.length - 1 );

            // UPDATE CURRENT ARTICLE
            outputArticleName.innerHTML = STATE.requestSettings.inputArticleValue.get();

            // UPDATE EDITS FROM ANONYMOUS
            var anymRate = RESPONSE.fraction_not_known * 100;
            outputAnymRate.innerHTML = anymRate;

            // UPDATE TAG CLOUD
            var data = new google.visualization.DataTable();
            data.addColumn('string', 'Label');
            data.addColumn('number', 'Value');
            //data.addColumn('string', 'Link');
            data.addRows(RESPONSE.most_specfic_terms.length);
            for (var i = 0; i < RESPONSE.most_specfic_terms.length; i++) {
                var term = RESPONSE.most_specfic_terms[i];
                data.setValue(i, 0, term.word);
                data.setValue(i, 1, term.tfidf);
                // data.setValue(i, 2, "some URL");
            };
            chartTagCloud.draw(data, null);

            // Update vAxis 
            // var peak = 0;
            // RESPONSE.revisions.map(function(v,i,a){
            //     v.summary.map(function(v,i,a){
            //         peak = v.editSize > peak ? v.editSize : peak;
            //     });
            // });
            // CHART_T5_OPTIONS.vAxis = {maxValue: peak, minValue: peak};

            updateUIStateForRevision(0);
        }
        r.send();
    }

    // On Timeslidervalue changed
    function onSlideValueChange(e, ui ) {
        // console.log(ui.value);
        updateUIStateForRevision(ui.value);
    }

    // Set UI State for a specific Revision
    function updateUIStateForRevision(revisionIndex) {
        // Prevent for null pointer
        if (!RESPONSE || RESPONSE.revisions.length < revisionIndex){return;}

        // UPDATE GEOCHART WITH DATA FROM FIRST TIMESTAMP
        var countries = new Array();
        var countriesTop5 = new Array();
        var totalEdits = 0;
        var authors = 0;
        var countryBucket;

        countries.push(['Country', 'Edits']);
        for (var j = 0; j < RESPONSE.revisions[revisionIndex].summary.length; j++) {
            countryBucket = RESPONSE.revisions[revisionIndex].summary[j];
            if (countryBucket.country == "" || 
                countryBucket.country == "NK") {
                countryBucket.country = _["unknown"][l];
            }
            // Push to Geo Data Array
            countries.push([
                countryBucket.country,
                countryBucket.editSize
                ]);
            // Update max number od edits
            if (countryBucket.editSize > 0) {
                totalEdits += countryBucket.editSize;
                authors++;
            };
        }
        // console.log(countries);
        var geodata = google.visualization.arrayToDataTable(countries);
        geochart.draw(geodata);

        // UPDATE TOP 5 CONTRIBUTORS
        // Sort Array and update Index
        countries.sort(function(a, b){return b[1]-a[1]});
        var colors = [{role:'style'},'#2e3143','#393d4f','#474c63','#5d6482','#646c8c'];
        for (var i = 0; i < countries.slice(0,6).length; i++) {
            countries[i].push(colors[i]);
        };
        var data = new google.visualization.arrayToDataTable(countries.slice(0,6));
        chartTop5.draw(data, CHART_T5_OPTIONS);

        // UPDATE DATE
        var d = new Date(RESPONSE.revisions[revisionIndex].timeStamp);
        var n = d.toUTCString();
        outputDate.innerHTML = n;

        // UPDATE EDITS
        outputEdits.innerHTML = totalEdits;

        // UPDATE AUTHORS
        // coming feature... >> authors
    }

    // Call function (fn) with a specific frequency (fps)
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
})();