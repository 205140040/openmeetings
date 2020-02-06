/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License") +  you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openmeetings.web.common;

import java.time.LocalDateTime;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.form.datetime.LocalDateTimeTextField;
import org.apache.wicket.model.IModel;

public class OmDateTimePicker extends AbstractOmDateTimePicker<LocalDateTime> {
	private static final long serialVersionUID = 1L;

	public OmDateTimePicker(String id, IModel<LocalDateTime> model) {
		super(id, model, getDateTimeFormat());
	}

	@Override
	protected Component newInput(String wicketId, String dateFormat) {
		return new LocalDateTimeTextField(wicketId, getModel(), dateFormat);
	}
}
