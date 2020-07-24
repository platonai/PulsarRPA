package ai.platon.pulsar.ql;

import ai.platon.pulsar.ql.types.ValueDom;
import ai.platon.pulsar.ql.types.ValueURI;
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

    /**
     * The list of types. An ArrayList so that Tomcat doesn't set it to null
     * when clearing references.
     */
    private static final ArrayList<DataType> TYPES = new ArrayList<>();
    private static final HashMap<Integer, TypeInfo> TYPE_INFO_BY_VALUE_TYPE = new HashMap<>();
    private static final HashMap<String, DataType> TYPES_BY_NAME = new HashMap<>();
    private static final HashMap<Integer, DataType> TYPES_BY_VALUE_TYPE = new HashMap<>();

    /** */
    public PulsarDataTypesHandler() {
        createDomDataType();
        createURIDataType();
    }

    /** Constructs data type instance for complex number type */
    private static void createDomDataType() {
        DataType dt = new DataType();
        dt.type = DOM_DATA_TYPE_ID;
        dt.name = DOM_DATA_TYPE_NAME;
        dt.sqlType = Types.JAVA_OBJECT;

        TYPE_INFO_BY_VALUE_TYPE.put(dt.type, ValueDom.typeInfo);
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
    public TypeInfo getTypeInfoById(int type, long precision, int scale, ExtTypeInfo extTypeInfo) {
        if (type == ValueDom.type) {
            return ValueDom.typeInfo;
        }
        return null;
    }

    @Override
    public String getDataTypeClassName(int type) {
        if (type == DOM_DATA_TYPE_ID) {
            return ValueDom.class.getName();
        } else if (type == URI_DATA_TYPE_ID) {
            return ValueURI.class.getName();
        }
        throw DbException.throwInternalError("type="+type);
    }

    @Override
    public int getTypeIdFromClass(Class<?> x) {
        if (ValueDom.class.isAssignableFrom(x)) {
            return DOM_DATA_TYPE_ID;
        } else if (ValueURI.class.isAssignableFrom(x)) {
            return URI_DATA_TYPE_ID;
        }
        return Value.JAVA_OBJECT;
    }

    @Override
    public Value convert(Value source, final int targetType) {
        if (source.getType().getValueType() == targetType) {
            return source;
        }

        if (targetType == DOM_DATA_TYPE_ID) {
            switch (source.getType().getValueType()) {
                case Value.JAVA_OBJECT: {
                    assert source instanceof ValueJavaObject;
                    return ValueDom.get(new String(source.getBytesNoCopy()));
                } case Value.STRING: {
                    assert source instanceof ValueString;
                    return ValueDom.get(source.getString());
                } case Value.BYTES: {
                    assert source instanceof ValueBytes;
                    return ValueDom.get(new String(source.getBytesNoCopy()));
                }
            }

            throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, source.getString());
        } else if (targetType == URI_DATA_TYPE_ID) {
            switch (source.getType().getValueType()) {
                case Value.JAVA_OBJECT: {
                    assert source instanceof ValueJavaObject;
                    return ValueURI.get(new String(source.getBytesNoCopy()));
                } case Value.STRING: {
                    assert source instanceof ValueString;
                    return ValueURI.get(source.getString());
                } case Value.BYTES: {
                    assert source instanceof ValueBytes;
                    return ValueURI.get(new String(source.getBytesNoCopy()));
                }
            }

            throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, source.getString());
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
        }
        return ValueJavaObject.getNoCopy(data, null, dataHandler);
    }

    @Override
    public Object getObject(Value value, Class<?> cls) {
        if (cls.equals(ValueDom.class)) {
            // System.out.println(value.getType() + ", " + DataType.getDataType(value.getType()).name);
            if (value.getType().getValueType() == DOM_DATA_TYPE_ID) {
                return value.getObject();
            }
//            else if (value.getType() == Value.NULL) {
//                return null;
//            }
            return convert(value, DOM_DATA_TYPE_ID).getObject();
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
