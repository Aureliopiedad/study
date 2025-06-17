# validator 获取国际化报错信息的问题

## 场景说明

在非http请求中，一个异步线程中校验了某个对象，需要通过websocket通知到前台，需要将校验信息填写进去，但是默认的validator只能获取一个语言的报错信息。

同时这个类又在整个项目的common中，被不同的模块引用，如果修改message信息的话，可能需要修改的地方较多。

不要和我说做两次valid不就行了，这样做看起来太蠢了。

## 源码说明

正常情况下spring boot项目在自动装配下会在`org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration`中生成一个默认的Validator的bean。

不管有没有使用spring boot，validator的底层实现都是`org.hibernate.validator.internal.engine.ValidatorImpl`，在这个类中会基于被校验的对象生成`org.hibernate.validator.internal.engine.validationcontext.ValidationContext`，默认的context的实现类是固定的`org.hibernate.validator.internal.engine.validationcontext.BeanValidationContext`。

在抽象类`org.hibernate.validator.internal.engine.validationcontext.AbstractValidationContext`中，记录了校验失败的原因，字段为`failingConstraintViolations`，其中校验失败的原因为String，意味着只能选择一个语言返回。

通过messageSource(不是spring boot的那个)获取失败原因的位置在`org.hibernate.validator.internal.engine.validationcontext.ValidationContext#addConstraintFailure`。

跳过中间乱七八糟的，直接看`javax.validation.MessageInterpolator#interpolate(java.lang.String, javax.validation.MessageInterpolator.Context)`。在抽象类`org.hibernate.validator.messageinterpolation.AbstractMessageInterpolator#interpolateMessage`中，是真正的逻辑，可以在`org.hibernate.validator.messageinterpolation.AbstractMessageInterpolator#resolveMessage`中看到，validator会获取三个ResourceBundle，分别是用户指定的、默认的、还有个contributed的不知道是什么。

拿到类似于`个数必须在{min}和{max}之间`的描述后，需要填充其中的占位符。validator使用`org.hibernate.validator.internal.engine.messageinterpolation.parser.TokenIterator`来做分割，并从注解中获取对应的property，来填充到这个描述信息中。

比较简单的做法是，直接使用validator写好的MessageInterpolator来做这个事情，当然也可以通过自定义的MessageInterpolator来实现。

## 实现方案

```java
@Configuration
public class ValidConfig {
    @Bean
    public MessageInterpolator customMessageInterpolator(ApplicationContext applicationContext) {
        MessageInterpolatorFactory interpolatorFactory = new MessageInterpolatorFactory(applicationContext);
        return interpolatorFactory.getObject();
    }
}

@Service
@Slf4j
public class ValidService {
    @Autowired
    private Validator validator;

    @Autowired
    private MessageInterpolator messageInterpolator;

    @PostConstruct
    public void init() {
        TestDTO testDTO = new TestDTO();
        testDTO.setId("123123123");

        Set<ConstraintViolation<TestDTO>> validate = validator.validate(testDTO);
        if (!validate.isEmpty()) {
            ConstraintViolation<TestDTO> testDTOConstraintViolation = validate.stream().findAny().get();

            CustomerContext customerContext = new CustomerContext(testDTOConstraintViolation.getConstraintDescriptor());

            log.info(messageInterpolator.interpolate(testDTOConstraintViolation.getMessageTemplate(), customerContext, Locale.SIMPLIFIED_CHINESE));
            log.info(messageInterpolator.interpolate(testDTOConstraintViolation.getMessageTemplate(), customerContext, Locale.US));
        }
    }
}
```