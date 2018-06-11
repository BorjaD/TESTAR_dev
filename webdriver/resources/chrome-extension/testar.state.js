var getStateTreeTestar = function (ignoredTags) {
    var body = document.body;
    var bodyWrapped = wrapElementTestar(body);
    traverseElementTestar(bodyWrapped, body, ignoredTags);
    return bodyWrapped;
};

function traverseElementTestar(parentWrapped, rootElement, ignoredTags) {
    var childNodes = parentWrapped.element.childNodes;
    for (var i = 0; i < childNodes.length; i++) {
        var childElement = childNodes[i];

        // Filter ignored tags or non-element nodes
        if (childElement.nodeType === 3) {
            parentWrapped.textContent += childElement.textContent;
            continue;
        }
        if (ignoredTags.includes(childElement.nodeName.toLowerCase()) ||
            childElement.nodeType !== 1) {
            continue
        }

        var childWrapped = wrapElementTestar(childElement);
        traverseElementTestar(childWrapped, rootElement, ignoredTags);
        parentWrapped.wrappedChildren.push(childWrapped);
    }
}

/*
 * Wrapping all required properties in a struct
 * This minimizes the roundtrips between Java and webdriver
 */
function wrapElementTestar(element) {
    var computedStyle = getComputedStyle(element);

    return {
        element: element,

        id: element.id,
        name: element.name,
        tagName: element.tagName.toLowerCase(),
        textContent: "",
        title: element.title,
        value: element.value,
        href: element.href,
        cssClasses: element.getAttribute("class"),
        display: computedStyle.getPropertyValue('display'),
        type: element.getAttribute("type"),

        zIndex: getZIndexTestar(element),
        rect: getRectTestar(element),
        dimensions: getDimensionsTestar(element),
        isBlocked: getIsBlockedTestar(element),
        isClickable: isClickableTestar(element),
        hasKeyboardFocus: document.activeElement === element,

        documentHasFocus: document.hasFocus(),
        documentTitle: document.title,

        wrappedChildren: []
    };
}

function getZIndexTestar(element) {
    if (element === document.body) {
        return 0;
    }

    var zIndex = getComputedStyle(element).getPropertyValue('z-index');
    if (isNaN(zIndex)) {
        return getZIndexTestar(element.parentNode) + 1;
    }
    return zIndex * 1;
}

function getRectTestar(element) {
    var rect = element.getBoundingClientRect();
    if (element === document.body) {
        rect = document.documentElement.getBoundingClientRect();
    }

    return [
        parseInt(rect.x),
        parseInt(rect.y),
        parseInt(element === document.body ? window.innerWidth : rect.width),
        parseInt(element === document.body ? window.innerHeight : rect.height)
    ];
}

function getDimensionsTestar(element) {
    if (element === document.body) {
        scrollLeft = Math.max(document.documentElement.scrollLeft, document.body.scrollLeft);
        scrollTop = Math.max(document.documentElement.scrollTop, document.body.scrollTop);
    }
    else {
        scrollLeft = element.scrollLeft;
        scrollTop = element.scrollTop;
    }

    var style = window.getComputedStyle(element);

    return {
        overflowX: style.getPropertyValue('overflow-x'),
        overflowY: style.getPropertyValue('overflow-y'),
        innerWidth: window.innerWidth,
        innerHeight: window.innerHeight,
        clientWidth: element.clientWidth,
        clientHeight: element.clientHeight,
        scrollWidth: element.scrollWidth,
        scrollHeight: element.scrollHeight,
        scrollLeft: scrollLeft,
        scrollTop: scrollTop
    };
}

function getIsBlockedTestar(element) {
    // get element at element's (click) position
    var rect = element.getBoundingClientRect();
    var x = rect.left + rect.width / 2;
    var y = rect.top + rect.height / 2;
    var elem = document.elementFromPoint(x, y);

    // return whether obscured element has same parent node
    // (will also return false if element === elem)
    if (elem === null) {
        return false;
    }
    return elem.parentNode !== element.parentNode;
}

function isClickableTestar(element) {
    // onClick defined as tag attribute
    if (element.onclick !== null) {
        return true;
    }
    // onClick added via JS
    var arr = element.getEventListeners('click');
    return arr !== undefined && arr.length > 0;
}
