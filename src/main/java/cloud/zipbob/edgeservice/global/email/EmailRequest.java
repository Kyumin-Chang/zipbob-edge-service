package cloud.zipbob.edgeservice.global.email;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailRequest {
    private String receiver;
    private String nickname;
    private String type; // welcome or goodbye
}
