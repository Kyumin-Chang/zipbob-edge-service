package cloud.zipbob.edgeservice.domain.member.exception;

import cloud.zipbob.edgeservice.global.exception.BaseException;
import cloud.zipbob.edgeservice.global.exception.BaseExceptionType;

public class MemberException extends BaseException {
    private final BaseExceptionType exceptionType;

    public MemberException(BaseExceptionType exceptionType) {
        this.exceptionType = exceptionType;
    }

    @Override
    public BaseExceptionType getExceptionType() {
        return exceptionType;
    }
}
