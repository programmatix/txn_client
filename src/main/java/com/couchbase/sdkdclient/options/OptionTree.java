package com.couchbase.sdkdclient.options;

import com.couchbase.sdkdclient.logging.LogUtil;
import org.slf4j.Logger;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Object which contains other options or other children OptionTree objects.
 * Tree objects are arranged in a heirarchy to enable similar names for
 * different groups; that is, to allow two {@link OptionConsumer} implementations
 * to declare an option {@code timeout}.
 *
 * In such case, each implementation may define its own {@link OptionPrefix}
 * which is applied during tree building time. When each implementation has its
 * own prefix, then the {@code timeout} option will be prefix and will thus
 * not conflict.
 *
 * <pre><blockquoute>{@code
 *  OptionTree fooTree = new OptionTree(new OptionPrefix("foo"));
 *  fooTree.addOption(new IntOption("timeout"));
 *
 *  OptionTree barTree = new OptionTree(new OptionPrefix("bar"));
 *  barTree.addOption(new IntOption("timeout"));
 *
 *  OptionTree root = new OptionTree();
 *  root.addChild(fooTree);
 *  root.addChild(barTree);
 *
 *  IntOption fooTimeout = root.find("foo/timeout");
 *  IntOption barTimeout = root.find("bar/timeout");
 * }</blockquoute></pre>
 */
public class OptionTree {

  /** Map of our direct children options */
  private final Map<String, RawOption> options = new HashMap<String, RawOption>();
  /** Set of our children trees */
  private final Set<OptionTree> children = new HashSet<OptionTree>();
  /** Link to our parent tree */
  private OptionTree parent;
  /** Our prefix */
  private OptionPrefix prefix;
  /** Human description */
  private final String description;
  /** Shorter description */
  private final String displayName;

  /** Logger */
  private final static Logger logger = LogUtil.getLogger(OptionTree.class);

  /**
   * @return the options for this node. This returns only the <b>direct</b>
   * descendent options added via {@link #addOption(RawOption)}. Use
   * {@link #getAllOptionsInfo()} for a method which returns options for
   * all nested trees as well.
   */
  public Collection<RawOption> getOptions() {
    return options.values();
  }

  /**
   * @return the children nodes for this node.
   */
  public Collection<OptionTree> getChildren() {
    return children;
  }

  /**
   * Construct a new tree node
   * @param parent_group The parent node
   * @param group_prefix The relative prefix of this tree.
   */
  OptionTree(OptionTree parent_group, OptionPrefix group_prefix, String group, String desc) {
    parent = parent_group;
    prefix = group_prefix;
    displayName = group;
    description = desc;
  }

  public OptionTree(OptionPrefix prefix) {
    this(null, prefix, "UNKNOWN", "FIXME: Undescribed");
  }

  /**
   * Create a tree with a {@link OptionPrefix#ROOT} prefix.
   */
  public OptionTree() {
    this(null, OptionPrefix.ROOT, "UNKNOWN", "FIXME: Undescribed");
  }

  /**
   * @return the parent tree, or {@code null} if there is no parent.
   */
  public OptionTree getParent() {
    if (parent == null) {
      return this;
    }
    return parent;
  }

  public OptionPrefix getPrefix() {
    return prefix;
  }

  public void setPrefix(OptionPrefix pfx) {
    prefix = pfx;
  }

  public String getDescription() {
    return description;
  }

  public String getGroupName() {
    return displayName;
  }

  public boolean isRoot() {
    return getParent() == this;
  }

  /**
   * Add an option to this tree. The option will inherit the tree's prefix.
   * @param opt The option to add.
   *
   * The added option will inherit the current value of {@link #getGroupName()}.
   */
  public void addOption(RawOption opt) {
    if (options.containsKey(opt.getName())) {
      throw new IllegalArgumentException("Duplicate option key: "+opt.getName());
    }
    options.put(opt.getName(), opt);
    if (!opt.hasCustomSubsystem()) {
      opt.setSubsystem(displayName);
    }
  }

  /**
   * Add a child node to this tree. The child will inherit this object's
   * prefix, if any.
   * @param tree The node to add
   */
  public void addChild(OptionTree tree) {
    tree.parent = this;
    children.add(tree);
  }

  /**
   * Loads an option collection from an object
   * @param o The object to scan
   * @param cls The class of the object to reflect. Because it is common
   *            for {@link OptionConsumer} implementations to have subclasses
   *            which add their own options, do <b>not</b> use {@code o.getClass()}
   */
  public static Collection<RawOption> extract(Object o, Class cls) {
    List<RawOption> ll = new ArrayList<RawOption>();

    for (Field fld : cls.getDeclaredFields()) {
      try {
        fld.getType().asSubclass(RawOption.class);
      } catch (ClassCastException ex) {
        continue;
      }

      fld.setAccessible(true);

      try {
        RawOption cur = (RawOption)fld.get(o);
        ll.add(cur);

      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return ll;
  }

  /**
   * Whether this node is a dummy node. A dummy node does not contain its
   * own prefix and does not contain any options of its own.
   * @return true if this tree is a 'dummy'
   */
  public boolean isDummy() {
    return prefix.getName().isEmpty() && options.isEmpty();
  }

  /**
   * Recurse through each option in the current level, determining if a
   * match has been found.
   * @param names
   * @param beginIx
   * @return the match, or null if not found.
   */
  private RawOption recurseInternal(String[] names, int beginIx) {
    RawOption ret;

    // Strip the leading prefix, if we're the first component..
    if (prefix != null && prefix.isRoot() == false && beginIx == 0 && names.length > 1) {
      logger.trace("Checking whether {} matches {}", names[beginIx], prefix);
      if (prefix.matches(names[beginIx])) {
        logger.trace("It does!");
        beginIx++;
      }
    }

    String curName = names[beginIx];
    // Last element. We should check if we have the element.
    if (isDummy() == false && beginIx == names.length - 1) {
      ret = options.get(curName);

      if (ret != null) {
        return ret;
      }

      for (RawOption cur : options.values()) {
        for (String aa : cur.getLongAliases()) {
          if (curName.equals(aa)) {
            return cur;
          }
        }
      }

      for (OptionTree child : children) {
        if (child.isDummy() || child.prefix == null || child.prefix.isRoot()) {
          ret = child.recurseInternal(names, beginIx);
          if (ret != null) {
            return ret;
          }
        }
      }

      return null;
    }

    for (OptionTree child : children) {
      if (child.isDummy()) {
        ret = child.recurseInternal(names, beginIx);
      } else if (beginIx < names.length-1) {
        ret = child.recurseInternal(names, beginIx + 1);
      } else {
        ret = null;
      }

      if (ret != null) {
        return ret;
      }
    }

    return null;
  }

  /**
   * Finds an option by its absolute name.
   * @param name The name, relative to this node, by which the option may
   * be found. Note that because the characters @{code [-_.]} are all 'normalized',
   * the {@code /} character should be used to delimit components.
   *
   * @return The option, or null if it cannot be found.
   */
  public RawOption find(String name) {
    name = name.replaceAll("^/+", "");
    name = RawOption.transformName(name);
    String[] names = name.split(OptionPrefix.PREFIX_DELIM);
    return recurseInternal(names, 0);
  }

  /**
   * Gets the hierarchy of all the option prefixes.
   * @return A collection of prefixes, with the first element being the root
   * prefix, and subsequent elements being the next prefixes.
   */
  public Collection<OptionPrefix> getHierarchy() {
    LinkedList<OptionPrefix> ll = new LinkedList<OptionPrefix>();
    for (OptionTree curTree = this; !curTree.isRoot(); curTree = curTree.parent) {
      if (curTree.prefix.getName().isEmpty()) {
        continue;
      }
      ll.addFirst(curTree.prefix);
    }

    ll.addFirst(OptionPrefix.ROOT);
    return ll;
  }

  private Collection<OptionInfo> getOwnOptionsInfo() {
    final StringBuilder sb = new StringBuilder();
    final OptionTree thisObj = this;
    ArrayList<OptionInfo> ret = new ArrayList<OptionInfo>(options.size());

    for (OptionPrefix pfx : getHierarchy()) {
      sb.append(pfx.getName()).append(OptionPrefix.PREFIX_DELIM);
    }

    final String hierString = sb.toString();

    for (final RawOption opt : options.values()) {
      OptionInfo info = new OptionInfo() {
        @Override
        public RawOption getOption() {
          return opt;
        }

        @Override
        public OptionTree getContainer() {
          return thisObj;
        }

        @Override
        public String getCanonicalName() {
          return hierString + opt.getName();
        }
      };
      ret.add(info);
    }
    return ret;
  }

  /**
   * @return A collection of all options contained within this tree. This
   * method will recurse through all children trees as well.
   */
  public Collection<OptionInfo> getAllOptionsInfo() {
    ArrayList<OptionInfo> ret = new ArrayList<OptionInfo>(getOwnOptionsInfo());
    for (OptionTree tree : getChildren()) {
      ret.addAll(tree.getAllOptionsInfo());
    }
    return ret;
  }

  /**
   * This method finds an option of a given type. The type must be a subclass
   * of a 'Generic' option
   * @param path The path to find (see {@link #find(String)} for more info)
   * @param cls Class object to use.
   * @param <T> The type of option to return
   * @return The found option.
   * @throws java.lang.IllegalArgumentException if the option does not exit.
   */
  public <T extends GenericOption> T findTypedOption(String path, Class<? extends T> cls) {
    RawOption opt = find(path);
    if (opt == null) {
      throw new IllegalArgumentException("No such path: "+ path);
    }
    return cls.cast(opt);
  }

  public void addString(String path,  String... values) {
    if (values.length == 0) {
      throw new IllegalArgumentException("Values cannot be empty");
    }

    RawOption opt = find(path);
    if (opt == null) {
      throw new IllegalArgumentException("No such path: " + path);
    }
    MultiOption mOpt = MultiOption.class.cast(opt);
    mOpt.addRawValues(values);
  }

  public void setString(String path, String value) {
    findTypedOption(path, StringOption.class).set(value);
  }

  public void setString(String path, StringOption src) {
    setString(path, src.getValue());
  }

  public void setInt(String path, int value) {
    findTypedOption(path, IntOption.class).set(value);
  }

  public void setInt(String path, IntOption src) {
    setInt(path, src.getValue());
  }

  public void setBool(String path, boolean value) {
    findTypedOption(path, BoolOption.class).set(value);
  }

  public void setBool(String path, BoolOption src) {
    setBool(path, src.getValue());
  }

  public <E extends Enum<E>> void setEnum(String path, E value) {
    @SuppressWarnings("unchecked")
    EnumOption<E> opt = findTypedOption(path, EnumOption.class);
    opt.set(value);
  }

  public void setFile(String path, File f) {
    findTypedOption(path, FileOption.class).set(f);
  }
}