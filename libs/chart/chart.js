window.onload = function () {
    var r = Raphael("holder", 620, 250),
        e = [],
        clr = [],
        months = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"],
        values = [],
        now = 0,
        bg = r.rect(243, 14, 134, 26, 13).attr({fill: "#666", stroke: "none"}),
        month = r.text(310, 27, months[now]).attr({fill: "#fff", stroke: "none", "font": '100 18px "Helvetica Neue", Helvetica, "Arial Unicode MS", Arial, sans-serif'}),
        rightc = r.circle(364, 27, 10).attr({fill: "#fff", stroke: "none"}),
        right = r.path("M360,22l10,5 -10,5z").attr({fill: "#000"}),
        leftc = r.circle(256, 27, 10).attr({fill: "#fff", stroke: "none"}),
        left = r.path("M260,22l-10,5 10,5z").attr({fill: "#000"}),
        c = r.path("M0,0").attr({fill: "none", "stroke-width": 3}),
        bg = r.path("M0,0").attr({stroke: "none", opacity: .3}),
        dotsy = [];
    function randomPath(length, j) {
        var path = "",
            x = 10,
            y = 0;
        dotsy[j] = dotsy[j] || [];
        for (var i = 0; i < length; i++) {
            dotsy[j][i] = Math.round(Math.random() * 200);
            if (i) {
                path += "C" + [x + 10, y, (x += 20) - 10, (y = 240 - dotsy[j][i]), x, y];
            } else {
                path += "M" + [10, (y = 240 - dotsy[j][i])];
            }
        }
        return path;
    }
    for (var i = 0; i < 12; i++) {
        values[i] = randomPath(30, i);
        clr[i] = Raphael.getColor(1);
    }
    c.attr({path: values[0], stroke: clr[0]});
    bg.attr({path: values[0] + "L590,250 10,250z", fill: clr[0]});
    // month.attr({fill: clr[0]});
    var animation = function () {
        var time = 500;
        if (now == 12) {
            now = 0;
        }
        if (now == -1) {
            now = 11;
        }
        c.animate({path: values[now], stroke: clr[now]}, time, "<>");
        bg.animate({path: values[now] + "L590,250 10,250z", fill: clr[now]}, time, "<>");
        month.attr({text: months[now]});
    };
    rightc.node.onclick = right.node.onclick = function () {
        now++;
        animation();
    };
    leftc.node.onclick = left.node.onclick = function () {
        now--;
        animation();
    };
    document.onclicks = function () {
        var path = r.parsePathString(values[now]),
            x = path[1][5] - 30,
            y = path[1][6];
        path = r.pathToRelative(path);
        path[1][0] = "m";
        path[1].splice(1, 5, 0);
        var newvalue = Math.round(Math.random() * 200) - 100;
        path = path.join(",") + "c10,0 10," + newvalue + " 20," + newvalue;
        c.animate({path: path}, 2000);
        r.safari();
    };
};
