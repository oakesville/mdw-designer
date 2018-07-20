package com.centurylink.mdw.designer.auth;

import com.centurylink.mdw.common.exception.DataAccessException;

public class TokenExpiryException extends DataAccessException {

    public TokenExpiryException(String msg) {
        super(msg);
    }

    public TokenExpiryException(String msg, Throwable t) {
        super(msg, t);
    }
}
