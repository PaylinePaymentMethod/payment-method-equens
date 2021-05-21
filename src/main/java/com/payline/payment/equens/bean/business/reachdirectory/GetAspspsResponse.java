package com.payline.payment.equens.bean.business.reachdirectory;

import com.google.gson.annotations.SerializedName;
import com.payline.payment.equens.bean.business.EquensApiMessage;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GetAspspsResponse extends EquensApiMessage {

    @SerializedName("ASPSP")
    private List<Aspsp> aspsps;

    GetAspspsResponse(EquensApiMessageBuilder builder) {
        super(builder);
    }
}
