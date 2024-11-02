package cloud.zipbob.edgeservice.auth.exception;

import cloud.zipbob.edgeservice.global.exception.BaseException;
import cloud.zipbob.edgeservice.global.exception.BaseExceptionType;

public class TokenException extends BaseException {
    private final BaseExceptionType exceptionType;

    public TokenException(BaseExceptionType exceptionType) {
        this.exceptionType = exceptionType;
    }

    @Override
    public BaseExceptionType getExceptionType() {
        return exceptionType;
    }
}
