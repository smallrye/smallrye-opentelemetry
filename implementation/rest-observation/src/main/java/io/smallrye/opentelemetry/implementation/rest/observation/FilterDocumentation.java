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
            return KeyName.merge(LowCardinalityValues.values(), ServerLowCardinalityValues.values());
        }

        @Override
        public KeyName[] getHighCardinalityKeyNames() {
            return HighCardinalityValues.values();
        }
    },
    CLIENT {
        @Override
        public Class<? extends ClientFilterConvention> getDefaultConvention() {
            return DefaultClientFilterConvention.class;
        }

        @Override
        public KeyName[] getLowCardinalityKeyNames() {
            return KeyName.merge(LowCardinalityValues.values(), ClientLowCardinalityValues.values());
        }

        @Override
        public KeyName[] getHighCardinalityKeyNames() {
            return HighCardinalityValues.values();
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
        URL_PATH {
            @Override
            public String asString() {
                return "url.path";
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

    public enum ServerLowCardinalityValues implements KeyName {
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

    public enum ClientLowCardinalityValues implements KeyName {
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
        }
    }
}
