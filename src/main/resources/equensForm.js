const $ = Payline.jQuery;
const aspspsList = [$BANKS_TO_REPLACE$];
const errorSubAspsp = "$ASPSP_MSG$";

function fillSubAspspId(subList) {
    subList.forEach(function(item) {
        $("[id$='subAspspId-options']").append($('<option>').val(item.label));
    });
}

function updateAspspOption(bank) {
    $("[id$='subAspspId-options']").empty();
    $("[id$='subAspspId-container']").hide();
    aspspsList.forEach(function(item) {
        if (bank === item.id) {
            $("[id$='subAspspId-container']").show();
            fillSubAspspId(item.subList);
        }
    });
}

function validatePaymentMethodForm() {
    let validation = true;
    let $subAspspSelect = $("[id$='subAspspId']");
    let $subAspspSelectContainer = $("[id$='subAspspId-container']");

    removeSubAspspMessage();
    let subAspspValue = $subAspspSelect.val();
    if ($($subAspspSelectContainer).is(":visible")) {
        let match = $("[id$='subAspspId-options'] option").filter(function() {
            return ($(this).val() === subAspspValue);
        });
        if (match.length === 0 ) {
            //afficher message d'erreur.
            addSubAspspErrorMessage();
            validation = false;
        }
    }
    return validation;
}

function addSubAspspErrorMessage() {
    let $subAspspContainer = $("[id$='subAspspId-container']");
    $subAspspContainer.prepend('<p id="subAspspId-message" class="pl-message pl-message-error">' + errorSubAspsp + '</p>');
    $subAspspContainer.removeClass('pl-has-no-error');
    $subAspspContainer.addClass('pl-has-error');

}

function removeSubAspspMessage() {
    let $subAspspMessage = $("#subAspspId-message");
    if ($subAspspMessage.length) {
        $subAspspMessage.remove();
    }
}

$(document).ready(function() {
    let $aspspSelect = $("[id$='aspspId']");
    let $subAspsp = $("[id$='subAspspId']");
    updateAspspOption($aspspSelect.val());
    $aspspSelect.change(function() {
        $("[id$='subAspspId']").val('');
        removeSubAspspMessage();
        let bank = $( this ).val();
        updateAspspOption(bank);
    });
    $subAspsp.change(function() {
        removeSubAspspMessage();
    })
});
