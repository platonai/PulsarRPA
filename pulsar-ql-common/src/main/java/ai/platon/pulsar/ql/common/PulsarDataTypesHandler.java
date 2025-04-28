package ai.platon.pulsar.ql.common;

import ai.platon.pulsar.ql.common.types.ValueDom;
import ai.platon.pulsar.ql.common.types.ValueStringJSON;
import ai.platon.pulsar.ql.common.types.ValueURI;
import org.h2.api.CustomDataTypesHandler;
import org.h2.api.ErrorCode;
import org.h2.message.DbException;
import org.h2.store.DataHandler;
import org.h2.value.*;
import org.jsoup.nodes.Element;

import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;

public class PulsarDataTypesHandler implements CustomDataTypesHandler {

    public final static String DOM_DATA_TYPE_NAME = "DOM";
    public final static int DOM_DATA_TYPE_ID = 1010;
    public final static int DOM_DATA_TYPE_ORDER = 100_010;

    public final static String URI_DATA_TYPE_NAME = "URI";
    public final static int URI_DATA_TYPE_ID = 1011;
    public final static int URI_DATA_TYPE_ORDER = 100_011;

    public final static String STRING_JSON_DATA_TYPE_NAME = "STRING_JSON";
    public final static int STRING_JSON_DATA_TYPE_ID = 1021;
    public final static int STRING_JSON_DATA_TYPE_ORDER = 100_021;

    /**
     * The list of types. An ArrayList so that Tomcat doesn't set it to null
     * when clearing references.
     */
    private static final ArrayList<DataType> TYPES = new ArrayList<>();
    private static final HashMap<String, DataType> TYPES_BY_NAME = new HashMap<>();
    private static final HashMap<Integer, DataType> TYPES_BY_VALUE_TYPE = new HashMap<>();

    /** */
    public PulsarDataTypesHandler() {
        createDomDataType();
        createURIDataType();
        createStringJSONDataType();
    }

    /** Constructs data type instance for complex number type */
    private static void createDomDataType() {
        DataType dt = new DataType();
        dt.type = DOM_DATA_TYPE_ID;
        dt.name = DOM_DATA_TYPE_NAME;
        dt.sqlType = Types.JAVA_OBJECT;

        register(dt);
    }

    /** Constructs data type instance for complex number type */
    private static void createURIDataType() {
        DataType dt = new DataType();
        dt.type = URI_DATA_TYPE_ID;
        dt.name = URI_DATA_TYPE_NAME;
        dt.sqlType = Types.JAVA_OBJECT;
        register(dt);
    }

    /** Constructs data type instance for complex number type */
    private static void createStringJSONDataType() {
        DataType dt = new DataType();
        dt.type = STRING_JSON_DATA_TYPE_ID;
        dt.name = STRING_JSON_DATA_TYPE_NAME;
        dt.sqlType = Types.JAVA_OBJECT;
        register(dt);
    }

    private static void register(DataType dt) {
        TYPES.add(dt);
        TYPES_BY_NAME.put(dt.name, dt);
        TYPES_BY_VALUE_TYPE.put(dt.type, dt);
    }

    @Override
    public DataType getDataTypeByName(String name) {
        return TYPES_BY_NAME.get(name);
    }

    @Override
    public DataType getDataTypeById(int type) {
        return TYPES_BY_VALUE_TYPE.get(type);
    }

    @Override
    public String getDataTypeClassName(int type) {
        if (type == DOM_DATA_TYPE_ID) {
            return ValueDom.class.getName();
        } else if (type == URI_DATA_TYPE_ID) {
            return ValueURI.class.getName();
        } else if (type == STRING_JSON_DATA_TYPE_ID) {
            return ValueStringJSON.class.getName();
        }
        throw DbException.throwInternalError("type="+type);
    }

    @Override
    public int getTypeIdFromClass(Class<?> x) {
        if (ValueDom.class.isAssignableFrom(x)) {
            return DOM_DATA_TYPE_ID;
        } else if (ValueURI.class.isAssignableFrom(x)) {
            return URI_DATA_TYPE_ID;
        } else if (ValueStringJSON.class.isAssignableFrom(x)) {
            return STRING_JSON_DATA_TYPE_ID;
        }
        return Value.JAVA_OBJECT;
    }

    @Override
    public Value convert(Value source, final int targetType) {
        if (source.getType() == targetType) {
            return source;
        }

        if (targetType == DOM_DATA_TYPE_ID) {
            return switch (source.getType()) {
                case Value.JAVA_OBJECT -> {
                    assert source instanceof ValueJavaObject;
                    yield ValueDom.get(new String(source.getBytesNoCopy()));
                }
                case Value.STRING -> {
                    assert source instanceof ValueString;
                    yield ValueDom.get(source.getString());
                }
                case Value.BYTES -> {
                    assert source instanceof ValueBytes;
                    yield ValueDom.get(new String(source.getBytesNoCopy()));
                }
                default -> throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, source.getString());
            };

        } else if (targetType == URI_DATA_TYPE_ID) {
            return switch (source.getType()) {
                case Value.JAVA_OBJECT -> {
                    assert source instanceof ValueJavaObject;
                    yield ValueURI.get(new String(source.getBytesNoCopy()));
                }
                case Value.STRING -> {
                    assert source instanceof ValueString;
                    yield ValueURI.get(source.getString());
                }
                case Value.BYTES -> {
                    assert source instanceof ValueBytes;
                    yield ValueURI.get(new String(source.getBytesNoCopy()));
                }
                default -> throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, source.getString());
            };

        } else if (targetType == STRING_JSON_DATA_TYPE_ID) {
            return switch (source.getType()) {
                case Value.JAVA_OBJECT -> {
                    assert source instanceof ValueJavaObject;
                    yield ValueStringJSON.get(new String(source.getBytesNoCopy()));
                }
                case Value.STRING -> {
                    assert source instanceof ValueString;
                    yield ValueStringJSON.get(source.getString());
                }
                case Value.BYTES -> {
                    assert source instanceof ValueBytes;
                    yield ValueStringJSON.get(new String(source.getBytesNoCopy()));
                }
                default -> throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, source.getString());
            };
        } else {
            return source.convertTo(targetType);
        }
    }

    @Override
    public int getDataTypeOrder(int type) {
        if (type == DOM_DATA_TYPE_ID) {
            return DOM_DATA_TYPE_ORDER;
        } else if (type == URI_DATA_TYPE_ID) {
            return URI_DATA_TYPE_ORDER;
        } else if (type == STRING_JSON_DATA_TYPE_ID) {
            return STRING_JSON_DATA_TYPE_ORDER;
        }

        throw DbException.get(ErrorCode.UNKNOWN_DATA_TYPE_1, "type:" + type);
    }

    @Override
    public Value getValue(int type, Object data, DataHandler dataHandler) {
        if (type == DOM_DATA_TYPE_ID) {
            assert data instanceof Element;
            return ValueDom.get((Element) data);
        } else if (type == URI_DATA_TYPE_ID) {
            assert data instanceof String;
            return ValueURI.get((String) data);
        } else if (type == STRING_JSON_DATA_TYPE_ID) {
            assert data instanceof String;
            return ValueStringJSON.get((String) data);
        }

        return ValueJavaObject.getNoCopy(data, null, dataHandler);
    }

    @Override
    public Object getObject(Value value, Class<?> cls) {
        if (cls.equals(ValueDom.class)) {
            if (value.getType() == DOM_DATA_TYPE_ID) {
                return value.getObject();
            }
            return convert(value, DOM_DATA_TYPE_ID).getObject();
        } else if (cls.equals(ValueURI.class)) {
            if (value.getType() == URI_DATA_TYPE_ID) {
                return value.getObject();
            }
            return convert(value, URI_DATA_TYPE_ID).getObject();
        } else if (cls.equals(ValueStringJSON.class)) {
            if (value.getType() == STRING_JSON_DATA_TYPE_ID) {
                return value.getObject();
            }
            return convert(value, STRING_JSON_DATA_TYPE_ID).getObject();
        }

        throw DbException.get(ErrorCode.UNKNOWN_DATA_TYPE_1, "type:" + value.getType());
    }

    @Override
    public boolean supportsAdd(int type) {
        if (type == DOM_DATA_TYPE_ID) {
            return false;
        } else if (type == Value.UNKNOWN) {
            return false;
        }
        return false;
    }

    @Override
    public int getAddProofType(int type) {
        if (type == DOM_DATA_TYPE_ID) {
            return type;
        }
        throw DbException.get(ErrorCode.UNKNOWN_DATA_TYPE_1, "type:" + type);
    }
}
