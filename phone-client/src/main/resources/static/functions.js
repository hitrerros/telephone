function handlePersonalDataFormSubmit({formId, nameInputId, numberInputId, endpoint}) {
    const form = document.getElementById(formId);
    if (!form) return;

    form.addEventListener("submit", function (e) {
        e.preventDefault();

        const name = document.getElementById(nameInputId).value;
        const number = document.getElementById(numberInputId).value;

        fetch(`${endpoint}/${encodeURIComponent(number)}/${encodeURIComponent(name)}`, {
            method: "GET"
        })
            .then(response => response.text())
            .then(data => typeOutText(data))
            .catch(error => {
                typeOutText("error occured: " + error.text);
            });
    });
}

function typeOutText(text) {
    const messageDiv = document.getElementById("sharedOutput");
    if (!messageDiv) return;

    messageDiv.innerHTML = `<div id="terminalOutput"></div>`;
    const outputDiv = document.getElementById("terminalOutput");
    let i = 0;

    const cursor = document.createElement("span");
    cursor.classList.add("blinking-cursor");
    outputDiv.appendChild(cursor);

    const typeInterval = setInterval(() => {
        if (i < text.length) {
            cursor.insertAdjacentText("beforebegin", text.charAt(i));
            i++;
        } else {
            clearInterval(typeInterval);
        }
    }, 30);
}

document.addEventListener("DOMContentLoaded", function () {
    handlePersonalDataFormSubmit({
        formId: "checkInForm",
        nameInputId: "checkInName",
        numberInputId: "checkInPhonenumber",
        endpoint: "/register"
    });


});

