/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.designer.display;

import java.util.List;

import com.centurylink.mdw.model.value.attribute.AttributeVO;

public interface Selectable {
    
    Long getId();
    
    String getName();
    void setName(String value);

    String getDescription();
    void setDescription(String value);

    // do not delete the followings - used by $+SLA for MDW 4
    int getSLA();
    void setSLA(int value);

    List<AttributeVO> getAttributes();
    String getAttribute(String name);
    void setAttribute(String name, String value);

}
