package som.interpreter.actors;

import som.VM;
import som.interpreter.actors.SPromise.SResolver;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.primitives.Primitive;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;


@GenerateNodeFactory
@Primitive("actorsResolve:with:")
public abstract class ResolvePromiseNode extends BinaryExpressionNode {

  public abstract Object executeEvaluated(final SResolver receiver, Object argument);

  // The DSL should check this guard only on node creation, and other wise turn it into an assertion
  protected static boolean hackGuardToTellTheVMThatWeExecuteMessages() {
    if (CompilerDirectives.inInterpreter()) {
      // we need to set this flag once, this should be reasonably OK
      VM.hasSendMessages();
    }
    return true;
  }

  @Specialization(guards = {"hackGuardToTellTheVMThatWeExecuteMessages()", "resolver.getPromise() == result"})
  public SResolver selfResolution(final SResolver resolver, final SPromise result) {
    return resolver;
  }

  @Specialization(guards = "resolver.getPromise() != result")
  public SResolver chainedPromise(final SResolver resolver, final SPromise result) {
    assert resolver.assertNotResolved();
    SPromise promise = resolver.getPromise();
    synchronized (promise) { // TODO: is this really deadlock free?
      result.addChainedPromise(promise);
    }
    return resolver;
  }

  protected static boolean notAPromise(final Object result) {
    return !(result instanceof SPromise);
  }

  @Specialization(guards = "notAPromise(result)")
  public SResolver normalResolution(final SResolver resolver, final Object result) {
     SResolver.resolveAndTriggerListeners(result, resolver.getPromise());
     return resolver;
  }
}