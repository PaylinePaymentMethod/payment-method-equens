const $ = Payline.jQuery;
const aspspsList = [$BANKS_TO_REPLACE$];

// Selection d'un ASPSP par rapport à son identifiant.
function getAspspById(aspspId) {
    let aspsp =  aspspsList.filter(function (elem) {
        return (elem.id === aspspId);
    });
    if (aspsp.length > 0 ) {
        return aspsp[0];
    }
}

//Selection de la valeur actuelle d'un SubAspsp
function getSubAspsp() {
    let aspspValue = $("[id$='aspspId']").val();
    let subAspspValue = $("[id$='subAspspId']").val();
    let result;
    let aspsp = getAspspById(aspspValue)
    if (!Payline.u.isUndefined(aspsp) && !Payline.u.isUndefined(aspsp.subList)&& aspsp.subList.length > 0) {
        result = aspsp.subList.filter(function (elem) {
            return (elem.label === subAspspValue);
        });
    }
    if (result.length > 0 ) {
        return result[0];
    }
}

//Mettre à jour la liste des aspspOption.
function updateAspspOption(bank) {
   let $subAspspContainer = $("[id$='subAspspId-container']");
    let aspsp = getAspspById(bank);
    $("[id$='subAspspId-options']").empty();
    $subAspspContainer.hide();

    if (!Payline.u.isUndefined(aspsp) && !Payline.u.isUndefined(aspsp.subList) && aspsp.subList.length > 0) {
        $subAspspContainer.show();
        aspsp.subList.forEach(function (item) {
            $("[id$='subAspspId-options']").append($('<option>').val(item.label));
        });
    }
    displayIbanContainer(aspsp);
}

//Affichage ou non du container Iban selon la banque passé en paramètre
function displayIbanContainer(bank) {
    if (!Payline.u.isUndefined(bank) && bank.iban) {
        $("[id$='iban-container']").show();
    } else {
        $("[id$='iban-container']").hide();
    }
}

function PMAPIEventUpdateDynamicFields() {
    let result = [];
    let subAspspField = {};
    let ibanField = {};
    let $subAspspSelectContainer = $("[id$='subAspspId-container']");
    let $ibanContainer = $("[id$='iban-container']");
    subAspspField.key = "subAspspId"
    ibanField.key="iban";
    subAspspField.required = !!$($subAspspSelectContainer).is(":visible");
    ibanField.required = !!$($ibanContainer).is(":visible");

    result[0] = subAspspField;
    result[1] = ibanField;

    return result;
}


$(document).ready(function() {
    let $aspspSelect = $("[id$='aspspId']");
    let $subAspspSelect = $("[id$='subAspspId']");
    updateAspspOption($aspspSelect.val());

    $aspspSelect.change(function() {
        $("[id$='subAspspId']").val('');
        let bank = $( this ).val();
        updateAspspOption(bank);
    });
    $subAspspSelect.change(function() {
        displayIbanContainer(getSubAspsp());
    })
});
