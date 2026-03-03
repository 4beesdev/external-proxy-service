package externalproxy.support;

import java.util.Map;

public class ApiErrorResponse {
    private String message;
    private String code;
    private Map<String, String> fieldErrors;

    public static ApiErrorResponse of(String code, String message) {
        ApiErrorResponse r = new ApiErrorResponse();
        r.code = code;
        r.message = message;
        return r;
    }

    public static ApiErrorResponse withFieldErrors(String code, String message, Map<String, String> fieldErrors) {
        ApiErrorResponse r = new ApiErrorResponse();
        r.code = code;
        r.message = message;
        r.fieldErrors = fieldErrors;
        return r;
    }

    public String getMessage() {
        return message;
    }

    public String getCode() {
        return code;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}

