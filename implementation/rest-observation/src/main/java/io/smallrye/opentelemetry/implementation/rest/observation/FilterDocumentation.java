package io.smallrye.opentelemetry.implementation.rest.observation;

import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.docs.ObservationDocumentation;
import io.smallrye.opentelemetry.implementation.rest.observation.client.ClientFilterConvention;
import io.smallrye.opentelemetry.implementation.rest.observation.client.DefaultClientFilterConvention;
import io.smallrye.opentelemetry.implementation.rest.observation.server.DefaultServerFilterConvention;
import io.smallrye.opentelemetry.implementation.rest.observation.server.ServerFilterConvention;

public enum FilterDocumentation implements ObservationDocumentation {
    SERVER {
        @Override
        public Class<? extends ServerFilterConvention> getDefaultConvention() {
            return DefaultServerFilterConvention.class;
        }

        @Override
        public KeyName[] getLowCardinalityKeyNames() {
            return LowCardinalityValues.values();
        }

        @Override
        public KeyName[] getHighCardinalityKeyNames() {
            return KeyName.merge(HighCardinalityValues.values(), ServerHighCardinalityValues.values());
        }
    },
    CLIENT {
        @Override
        public Class<? extends ClientFilterConvention> getDefaultConvention() {
            return DefaultClientFilterConvention.class;
        }

        @Override
        public KeyName[] getLowCardinalityKeyNames() {
            return LowCardinalityValues.values();
        }

        @Override
        public KeyName[] getHighCardinalityKeyNames() {
            return KeyName.merge(HighCardinalityValues.values(), ClientHighCardinalityValues.values());
        }
    };

    public enum LowCardinalityValues implements KeyName {
        /**
         * The HTTP method of the request.
         */
        HTTP_REQUEST_METHOD {
            @Override
            public String asString() {
                return "http.request.method";
            }
        },
        HTTP_ROUTE {
            @Override
            public String asString() {
                return "http.route";
            }
        },
        URL_SCHEME {
            @Override
            public String asString() {
                return "url.scheme";
            }
        },
        HTTP_RESPONSE_STATUS_CODE {
            @Override
            public String asString() {
                return "http.response.status_code";
            }
        },
        NETWORK_PROTOCOL_NAME {
            @Override
            public String asString() {
                return "network.protocol.name";
            }
        },
        NETWORK_PROTOCOL_VERSION {
            @Override
            public String asString() {
                return "network.protocol.version";
            }
        }
    }

    public enum ServerHighCardinalityValues implements KeyName {
        SERVER_PORT {
            @Override
            public String asString() {
                return "server.port";
            }
        },
        SERVER_ADDRESS {
            @Override
            public String asString() {
                return "server.address";
            }
        }
    }

    public enum ClientHighCardinalityValues implements KeyName {
        CLIENT_ADDRESS {
            @Override
            public String asString() {
                return "client.address";
            }
        },
        CLIENT_PORT {
            @Override
            public String asString() {
                return "client.port";
            }
        }
    }

    public enum HighCardinalityValues implements KeyName {
        URL_PATH {
            @Override
            public String asString() {
                return "url.path";
            }
        },
        URL_QUERY {
            @Override
            public String asString() {
                return "url.query";
            }
        },
        ERROR {
            @Override
            public String asString() {
                return "error";
            }
        },
        URL_FULL {
            @Override
            public String asString() {
                return "url.full";
            }
        },
        USER_AGENT_ORIGINAL {
            @Override
            public String asString() {
                return "user_agent.original";
            }
        }
    }
}
