package uk.badamson.mc.presentation;
/*
 * © Copyright Benedict Adamson 2020-23.
 *
 * This file is part of MC.
 *
 * MC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with MC.  If not, see <https://www.gnu.org/licenses/>.
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests {@link PresentationLayerSpringConfiguration}
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class ObjectMapperTest {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Tests for java.time.{@link Instant}.
     */
    @Nested
    public class JavaTimeInstant {
        @Test
        public void canDeserialize() {
            final var javaType = objectMapper.constructType(Instant.class);
            assertThat(objectMapper.canDeserialize(javaType), is(true));
        }

        @Test
        public void canSerialize() {
            assertThat(objectMapper.canSerialize(Instant.class), is(true));
        }

    }
}
